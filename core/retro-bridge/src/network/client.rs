use std::collections::HashMap;
use std::error::Error;
use std::net::UdpSocket;
use std::sync::{Arc, Mutex, RwLock};
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use renet::{ConnectionConfig, RenetClient};
use renet::DefaultChannel::ReliableOrdered;
use renet_netcode::{ClientAuthentication, ConnectToken, NetcodeClientTransport};
use tracing::warn;
use crate::util::image::NativeImage;

static INSTANCE: Mutex<Option<Arc<RetroClient>>> = Mutex::new(None);

pub struct RetroClient {
    client: Mutex<RenetClient>,
    transport: Mutex<NetcodeClientTransport>,

    displays: RwLock<HashMap<i32, Mutex<NativeImage>>>,
    shutdown_requested: AtomicBool,
}

impl RetroClient {
    pub fn with_instance<T>(func: impl FnOnce(&mut Arc<RetroClient>) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        let mut guard = INSTANCE.lock()?;
        if let Some(instance) = guard.as_mut() {
            return func(instance);
        }
        Err("Instance not found!".into())
    }

    pub fn init(token: Vec<u8>) -> Result<(), Box<dyn Error>> {
        let mut guard = INSTANCE.lock()?;
        *guard = Some(Arc::new(RetroClient::new(token)));
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
        }
    }

    pub fn main_loop() {
        let mut next = Instant::now();
        let delta = Duration::from_micros(1000000 / 60);

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

        let mut guard = INSTANCE.lock().unwrap();
        *guard = None;
    }

    fn handle_packet(&self, data: Vec<u8>) {
        todo!()
    }

    pub fn is_connected(&self) -> bool {
        self.client.lock().unwrap().is_connected()
    }
}

// TODO: uint32_t* RetroClient::registerDisplay(const jUUID *uuid, int width, int height, uint32_t *data, int sampleRate, int codec) {
// TODO: void RetroClient::unregisterDisplay(const jUUID* uuid) {
// TODO: std::shared_ptr<NativeImage> RetroClient::getDisplay(const jUUID *uuid) {
// TODO: void RetroClient::sendControlsUpdate(const jUUID *link, const int port, const int16_t controls) {
// TODO: void RetroClient::onConnect() {
// TODO: void RetroClient::onDisconnect() {
// TODO: void RetroClient::onMessage(const ENetPacket *packet) {
