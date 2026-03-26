use crate::network::network_definitions::{PacketType, RETRO_PROTOCOL};
use rand::TryRng;
use renet::{ClientId, ConnectionConfig, RenetServer};
use renet_netcode::{ConnectToken, NetcodeServerTransport, ServerAuthentication, ServerConfig};
use std::error::Error;
use std::net::{SocketAddr, UdpSocket};
use std::sync::atomic::{AtomicBool, AtomicI32, AtomicU64, Ordering};
use parking_lot::{Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use renet::DefaultChannel::ReliableOrdered;
use tracing::{info, warn};
use crate::platform::generic_console::ConsoleRegistry;

static INSTANCE: RwLock<Option<RetroServer>> = RwLock::new(None);

static SHUTDOWN_REQUESTED: AtomicBool = AtomicBool::new(false);
static RUNNING_LOOP_COUNT: AtomicI32 = AtomicI32::new(0);
static CLIENT_ID_INCREMENTOR: AtomicU64 = AtomicU64::new(0);

pub struct RetroServer {
    server: Mutex<RenetServer>,
    transport: Mutex<NetcodeServerTransport>,

    public_addresses: Vec<SocketAddr>,
    private_key: [u8; 32],
}

impl RetroServer {
    pub fn with_instance<T>(func: impl FnOnce(&RetroServer) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        if let Some(instance) = INSTANCE.read().as_ref() {
            return func(instance);
        }
        Err("Instance not found!".into())
    }

    pub fn init(max_users: i32, bind: &str, addresses: Vec<String>) -> Result<(), Box<dyn Error>> {
        INSTANCE.write().replace(RetroServer::new(max_users, bind, addresses)?);
        Ok(())
    }

    pub fn deinit() -> Result<(), Box<dyn Error>> {
        SHUTDOWN_REQUESTED.store(true, Ordering::Relaxed);
        loop {
            if RUNNING_LOOP_COUNT.load(Ordering::Relaxed) <= 0 {
                break;
            }
        }
        info!("Disposing RetroServer instance: all loops finished");
        INSTANCE.write().take();
        Ok(())
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

        SHUTDOWN_REQUESTED.store(false, Ordering::SeqCst);
        RUNNING_LOOP_COUNT.store(0, Ordering::SeqCst);
        CLIENT_ID_INCREMENTOR.store(0, Ordering::SeqCst);

        Ok(Self {
            server: Mutex::new(RenetServer::new(ConnectionConfig::default())),
            transport: Mutex::new(NetcodeServerTransport::new(config, socket)?),

            public_addresses,
            private_key,
        })
    }

    pub fn gen_token(&self) -> Result<Vec<u8>, Box<dyn Error>> {
        let now = SystemTime::now().duration_since(UNIX_EPOCH)?;
        let client_id = CLIENT_ID_INCREMENTOR.fetch_add(1, Ordering::Relaxed);

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

        Self::with_instance(|instance| {
            RUNNING_LOOP_COUNT.fetch_add(1, Ordering::Relaxed);
            Ok(())
        }).unwrap();

        loop {
            next += delta;

            if !Self::with_instance(|instance| {
                if SHUTDOWN_REQUESTED.load(Ordering::Relaxed) {
                    return Ok(false);
                }

                let mut server = instance.server.lock();
                let mut transport = instance.transport.lock();

                transport.update(delta, &mut server)?;
                server.update(delta);

                for client in server.clients_id() {
                    while let Some(msg) = server.receive_message(client, ReliableOrdered) {
                        instance.handle_packet(client, msg.to_vec());
                    }
                }

                let packets: Mutex<Vec<Vec<u8>>> = Mutex::new(Vec::new());
                ConsoleRegistry::foreach(|console| {
                    while let Some(pak) = console.retrieve_video_packet() {
                        packets.lock().push(pak);
                    }
                    while let Some(pak) = console.encode_audio_packet() {
                        packets.lock().push(pak);
                    }
                });

                for packet in packets.lock().iter() {
                    for client in server.clients_id() {
                        server.send_message(client, ReliableOrdered, packet.clone());
                    }
                }

                transport.send_packets(&mut server);
                Ok(true)
            }).expect("Failed serverside packet handling frame") {
                break;
            }

            let now = Instant::now();
            if now > next {
                warn!("RetroServer main loop lagging behind!");
                next = now;
            } else {
                std::thread::sleep(next - now);
            }
        }

        Self::with_instance(|instance| {
            instance.server.lock().disconnect_all();

            RUNNING_LOOP_COUNT.fetch_sub(1, Ordering::Relaxed);
            SHUTDOWN_REQUESTED.store(true, Ordering::Relaxed);
            Ok(())
        }).expect("Failed to shutdown clientside connection");
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

                ConsoleRegistry::with_console(id, |console| {
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
        let mut next = Instant::now();
        let delta = Duration::from_micros(1000000 / 60);

        RUNNING_LOOP_COUNT.fetch_add(1, Ordering::Relaxed);

        loop {
            next += delta;

            if SHUTDOWN_REQUESTED.load(Ordering::Relaxed) {
                break;
            }

            ConsoleRegistry::foreach(|console| {
                console.encode_video_frame();
            });

            let now = Instant::now();
            if now > next {
                warn!("RetroServer video packing loop lagging behind!");
                next = now;
            } else {
                std::thread::sleep(next - now);
            }
        }

        RUNNING_LOOP_COUNT.fetch_sub(1, Ordering::Relaxed);
    }
}
