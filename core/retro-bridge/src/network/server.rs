use crate::network::network_definitions::{PacketType, RETRO_PROTOCOL};
use rand::TryRng;
use renet::{ClientId, ConnectionConfig, RenetServer};
use renet_netcode::{ConnectToken, NetcodeServerTransport, ServerAuthentication, ServerConfig};
use std::error::Error;
use std::net::{SocketAddr, UdpSocket};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use renet::DefaultChannel::ReliableOrdered;
use tracing::warn;
use crate::platform::generic_console::ConsoleRegistry;

static INSTANCE: RwLock<Option<Arc<RwLock<RetroServer>>>> = RwLock::new(None);

pub struct RetroServer {
    server: Mutex<RenetServer>,
    transport: Mutex<NetcodeServerTransport>,

    shutdown_requested: AtomicBool,
    client_id_incrementor: AtomicU64,

    public_addresses: Vec<SocketAddr>,
    private_key: [u8; 32],
}

impl RetroServer {
    pub fn with_instance<T>(func: impl FnOnce(&mut RetroServer) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        let mut guard = INSTANCE.write()?;
        if let Some(instance) = guard.as_mut() {
            return func(instance.write().as_mut().unwrap());
        }
        Err("Instance not found!".into())
    }

    pub fn init(max_users: i32, bind: &str, addresses: Vec<String>) -> Result<(), Box<dyn Error>> {
        let mut guard = INSTANCE.write()?;
        *guard = Some(Arc::new(RwLock::new(RetroServer::new(max_users, bind, addresses)?)));
        Ok(())
    }

    pub fn deinit() -> Result<(), Box<dyn Error>> {
        Self::with_instance(|instance| {
            instance.shutdown_requested.store(true, Ordering::Relaxed);
            Ok(())
        })
    }

    fn new(max_users: i32, bind: &str, addresses: Vec<String>) -> Result<Self, Box<dyn Error>> {
        let mut public_addresses: Vec<SocketAddr> = Vec::with_capacity(addresses.len());
        for address in addresses {
            public_addresses.push(address.parse()?)
        }

        let mut private_key = [0u8; 32];
        rand::rng().try_fill_bytes(private_key.as_mut())?;

        let socket = UdpSocket::bind(bind)?;
        let config = ServerConfig {
            current_time: SystemTime::now().duration_since(UNIX_EPOCH)?,
            max_clients: max_users as usize,
            protocol_id: RETRO_PROTOCOL,
            public_addresses: public_addresses.clone(),
            authentication: ServerAuthentication::Secure { private_key },
        };

        Ok(Self {
            server: Mutex::new(RenetServer::new(ConnectionConfig::default())),
            transport: Mutex::new(NetcodeServerTransport::new(config, socket)?),

            shutdown_requested: AtomicBool::new(false),
            client_id_incrementor: AtomicU64::new(0),

            public_addresses,
            private_key,
        })
    }

    pub fn gen_token(&mut self) -> Result<Vec<u8>, Box<dyn Error>> {
        let now = SystemTime::now().duration_since(UNIX_EPOCH)?;
        let client_id = self.client_id_incrementor.fetch_add(1, Ordering::Relaxed);

        let token = ConnectToken::generate(
            now, RETRO_PROTOCOL, 30, client_id, 15,
            self.public_addresses.clone(), None, &self.private_key
        )?;

        let mut buf: Vec<u8> = Vec::with_capacity(2048);
        token.write(&mut buf)?;
        Ok(buf.to_vec())
    }

    pub fn main_loop() {
        let mut next = Instant::now();
        let delta = Duration::from_millis(10);
        //let delta = Duration::from_micros(1000000 / 60);

        loop {
            next += delta;

            if !Self::with_instance(|instance| {
                if instance.shutdown_requested.load(Ordering::Relaxed) {
                    return Ok(false);
                }

                let mut server = instance.server.lock().unwrap();
                let mut transport = instance.transport.lock().unwrap();

                transport.update(delta, &mut server)?;
                server.update(delta);

                for client in server.clients_id() {
                    while let Some(msg) = server.receive_message(client, ReliableOrdered) {
                        instance.handle_packet(client, msg.to_vec());
                    }
                }

                let packets: Mutex<Vec<Vec<u8>>> = Mutex::new(Vec::new());
                ConsoleRegistry::foreach_mut(|console| {
                    while let Some(pak) = console.retrieve_video_packet() {
                        packets.lock().unwrap().push(pak);
                    }
                });

                for packet in packets.lock().unwrap().iter() {
                    for client in server.clients_id() {
                        server.send_message(client, ReliableOrdered, packet.clone());
                    }
                }

                // TODO: Send encoded audio packets

                transport.send_packets(&mut server);
                Ok(true)
            }).expect("Failed clientside packet handling frame") {
                break;
            }

            let now = Instant::now();
            if now > next {
                warn!("RetroClient main loop lagging behind!");
                next = now;
            } else {
                std::thread::sleep(next - now);
            }
        }

        Self::with_instance(|instance| {
            instance.shutdown_requested.store(true, Ordering::Relaxed);
            instance.server.lock().unwrap().disconnect_all();
            drop(instance.server.lock().unwrap());
            Ok(())
        }).expect("Failed to shutdown clientside connection");

        let mut guard = INSTANCE.write().unwrap();
        *guard = None;
    }

    fn handle_packet(&self, client: ClientId, mut data: Vec<u8>) {
        let packet_type: PacketType = From::from(data[0]);
        data.remove(0);

        match packet_type {
            PacketType::Controls => {
                if data.len() != 8 {
                    warn!("Received invalid control packet from {:?}", client);
                    return;
                }

                let id = i32::from_le_bytes([data[0], data[1], data[2], data[3]]);
                let port = i16::from_le_bytes([data[4], data[5]]);
                let data = i16::from_le_bytes([data[6], data[7]]);

                ConsoleRegistry::with_console_mut(id, |console| {
                    console.submit_input(port, data);
                    Ok(())
                }).expect("Failed to submit input");
            }
            _ => {
                warn!("Received invalid packet type (client)")
            }
        }
    }

    pub fn video_packing_loop() {
        todo!()
    }
}
