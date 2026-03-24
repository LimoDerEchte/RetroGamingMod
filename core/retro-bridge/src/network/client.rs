use std::collections::{HashMap};
use std::error::Error;
use std::net::UdpSocket;
use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use parking_lot::{Mutex, RwLock};
use renet::{ConnectionConfig, RenetClient};
use renet::DefaultChannel::ReliableOrdered;
use renet_netcode::{ClientAuthentication, ConnectToken, NetcodeClientTransport};
use tracing::{info, warn};
use crate::network::network_definitions::PacketType;
use crate::util::display::NativeDisplay;

static INSTANCE: RwLock<Option<RetroClient>> = RwLock::new(None);

pub struct RetroClient {
    client: Mutex<RenetClient>,
    transport: Mutex<NetcodeClientTransport>,

    displays: RwLock<HashMap<i32, Mutex<NativeDisplay>>>,
    running_loop_count: AtomicI32,
    shutdown_requested: AtomicBool,

    input_packet: Mutex<Option<Vec<u8>>>,
}

impl RetroClient {
    pub fn with_instance<T>(func: impl FnOnce(&RetroClient) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        if let Some(instance) = INSTANCE.read().as_ref() {
            return func(instance);
        }
        Err("Instance not found!".into())
    }

    pub fn with_display<T>(&self, id: i32, func: impl FnOnce(&mut NativeDisplay) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error + '_>> {
        if let Some(display) = self.displays.read().get(&id) {
            return func(&mut display.lock());
        }
        Err(format!("Display not found: {}", id).into())
    }

    pub fn init(token: Vec<u8>) {
        let mut guard = INSTANCE.write();
        *guard = Some(RetroClient::new(token));
    }

    pub fn deinit() -> Result<(), Box<dyn Error>> {
        Self::with_instance(|instance| {
            instance.shutdown_requested.store(true, Ordering::Relaxed);
            Ok(())
        })?;
        loop {
            if Self::with_instance(|instance| {
                Ok(instance.running_loop_count.load(Ordering::Relaxed))
            })? <= 0 {
                break;
            }
            std::thread::sleep(Duration::from_millis(100));
        }
        info!("Disposing RetroClient instance: all loops finished");
        let mut guard = INSTANCE.write();
        *guard = None;
        Ok(())
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
            running_loop_count: AtomicI32::new(0),
            shutdown_requested: AtomicBool::new(false),

            input_packet: Mutex::new(None),
        }
    }

    pub fn register_id(&self, id: i32, width: i32, height: i32, video_codec: i32, display_data_ptr: i64, audio_codec: i32) {
        self.displays.write().insert(id, Mutex::new(NativeDisplay::new(width, height, video_codec, display_data_ptr)));
    }

    pub fn unregister_id(&self, id: i32) {
        self.displays.write().remove(&id);
    }

    pub fn send_input_data(&self, id: i32, port: i16, data: i16) {
        let mut pak = Vec::with_capacity(7);

        pak.push(PacketType::Controls as u8);
        pak.extend_from_slice(&id.to_le_bytes());
        pak.extend_from_slice(&port.to_le_bytes());
        pak.extend_from_slice(&data.to_le_bytes());

        let mut lock = self.input_packet.lock();
        *lock = Some(pak);
    }

    pub fn main_loop() {
        let mut next = Instant::now();
        let delta = Duration::from_millis(10);

        Self::with_instance(|instance| {
            instance.running_loop_count.fetch_add(1, Ordering::Relaxed);
            Ok(())
        }).unwrap();

        loop {
            next += delta;

            if !Self::with_instance(|instance| {
                if instance.shutdown_requested.load(Ordering::Relaxed) {
                    return Ok(false);
                }

                let mut client = instance.client.lock();
                let mut transport = instance.transport.lock();

                transport.update(delta, &mut client)?;
                client.update(delta);

                if !client.is_connected() {
                    return Ok(true);
                }

                while let Some(msg) = client.receive_message(ReliableOrdered) {
                    instance.handle_packet(msg.to_vec());
                }

                if let Some(packet) = { instance.input_packet.lock().take() } {
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
            instance.running_loop_count.fetch_sub(1, Ordering::Relaxed);
            instance.shutdown_requested.store(true, Ordering::Relaxed);

            instance.client.lock().disconnect();
            Ok(())
        }).expect("Failed to shutdown clientside connection");
    }

    fn handle_packet(&self, mut data: Vec<u8>) {
        let packet_type: PacketType = From::from(data[0]);
        data.remove(0);

        match packet_type {
            PacketType::VideoData => {
                let stream = i32::from_be_bytes([data.remove(0), data.remove(0), data.remove(0), data.remove(0)]);
                let _ = self.with_display(stream, |display| {
                    display.receive(data);
                    Ok(())
                });
            }
            PacketType::AudioData => {
                todo!()
            }
            _ => {
                warn!("Received invalid packet type (client)")
            }
        }
    }

    pub fn video_receiving_loop() {
        let mut next = Instant::now();
        let delta = Duration::from_micros(1000000 / 60);

        Self::with_instance(|instance| {
            instance.running_loop_count.fetch_add(1, Ordering::Relaxed);
            Ok(())
        }).unwrap();

        loop {
            next += delta;

            if !Self::with_instance(|instance| {
                if instance.shutdown_requested.load(Ordering::Relaxed) {
                    return Ok(false);
                }

                let displays = instance.displays.read();
                for display in displays.values() {
                    display.lock().try_transmit();
                }

                Ok(true)
            }).expect("Failed clientside video receiving frame") {
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
            instance.running_loop_count.fetch_sub(1, Ordering::Relaxed);
            Ok(())
        }).unwrap();
    }

    pub fn is_connected(&self) -> bool {
        self.client.lock().is_connected()
    }
}
