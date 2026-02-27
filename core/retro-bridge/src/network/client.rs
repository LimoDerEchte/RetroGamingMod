use std::collections::{HashMap};
use std::error::Error;
use std::net::UdpSocket;
use std::sync::{Arc, Mutex, RwLock};
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use renet::{ConnectionConfig, DefaultChannel, RenetClient};
use renet::DefaultChannel::ReliableOrdered;
use renet_netcode::{ClientAuthentication, ConnectToken, NetcodeClientTransport};
use tracing::warn;
use crate::network::network_definitions::PacketType;
use crate::util::display::NativeDisplay;

static INSTANCE: RwLock<Option<Arc<RwLock<RetroClient>>>> = RwLock::new(None);

pub struct RetroClient {
    client: Mutex<RenetClient>,
    transport: Mutex<NetcodeClientTransport>,

    displays: RwLock<HashMap<i16, Mutex<NativeDisplay>>>,
    shutdown_requested: AtomicBool,

    input_packet: Option<Vec<u8>>,
}

impl RetroClient {
    pub fn with_instance<T>(func: impl FnOnce(&mut RetroClient) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        let mut guard = INSTANCE.write()?;
        if let Some(instance) = guard.as_mut() {
            return func(instance.write().as_mut().unwrap());
        }
        Err("Instance not found!".into())
    }

    fn with_display(&self, id: i16, func: impl FnOnce(&mut NativeDisplay)) {
        let guard = self.displays.read().unwrap();
        if let Some(display) = guard.get(&id) {
            func(display.lock().as_mut().unwrap());
        }
    }

    pub fn init(token: Vec<u8>) -> Result<(), Box<dyn Error>> {
        let mut guard = INSTANCE.write()?;
        *guard = Some(Arc::new(RwLock::new(RetroClient::new(token))));
        Ok(())
    }

    pub fn deinit() -> Result<(), Box<dyn Error>> {
        Self::with_instance(|instance| {
            instance.shutdown_requested.store(true, Ordering::Relaxed);
            Ok(())
        })
    }

    fn new(token: Vec<u8>) -> Self {
        let now = SystemTime::now().duration_since(UNIX_EPOCH).unwrap();
        let token = ConnectToken::read(&mut token.as_slice()).unwrap();
        let socket = UdpSocket::bind("0.0.0.0:0").unwrap();
        let transport = NetcodeClientTransport::new(now, ClientAuthentication::Secure { connect_token: token }, socket).unwrap();

        Self {
            client: Mutex::new(RenetClient::new(ConnectionConfig::default())),
            transport: Mutex::new(transport),

            displays: RwLock::new(Default::default()),
            shutdown_requested: AtomicBool::new(false),

            input_packet: None,
        }
    }

    pub fn register_id(&mut self, id: i16, width: i32, height: i32, codec: i32, display_data_ptr: i32) {
        self.displays.write().unwrap().insert(id, Mutex::new(NativeDisplay::new(width, height, codec, display_data_ptr)));
    }

    pub fn unregister_id(&mut self, id: i16) {
        self.displays.write().unwrap().remove(&id);
    }

    pub fn send_input_data(&mut self, id: i16, port: i16, data: i16) {
        let mut pak = Vec::with_capacity(7);

        pak.push(PacketType::Controls as u8);
        pak.extend_from_slice(&id.to_le_bytes());
        pak.extend_from_slice(&port.to_le_bytes());
        pak.extend_from_slice(&data.to_le_bytes());

        self.input_packet = Some(pak);
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

                let mut client = instance.client.lock().unwrap();
                let mut transport = instance.transport.lock().unwrap();

                transport.update(delta, &mut client)?;
                client.update(delta);

                if !client.is_connected() {
                    return Ok(false);
                }

                while let Some(msg) = client.receive_message(ReliableOrdered) {
                    instance.handle_packet(msg.to_vec());
                }

                if let Some(packet) = instance.input_packet.take() {
                    client.send_message(ReliableOrdered, packet);
                }

                transport.send_packets(&mut client)?;
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
            instance.client.lock().unwrap().disconnect();
            Ok(())
        }).expect("Failed to shutdown clientside connection");

        let mut guard = INSTANCE.write().unwrap();
        *guard = None;
    }

    fn handle_packet(&self, mut data: Vec<u8>) {
        let packet_type: PacketType = From::from(data[0]);
        data.remove(0);

        match packet_type {
            PacketType::Kick => {
                warn!("Received kick packet: {:?}", String::from_utf8_lossy(data.as_slice()));
            }
            PacketType::VideoData => {
                let stream = i16::from_be_bytes([data.remove(0), data.remove(0)]);
                self.with_display(stream, |display| {
                    display.receive(data);
                });
            }
            PacketType::AudioData => {
                todo!()
            }
            PacketType::Controls | PacketType::Invalid => {}
        }
    }

    pub fn is_connected(&self) -> bool {
        self.client.lock().unwrap().is_connected()
    }
}
