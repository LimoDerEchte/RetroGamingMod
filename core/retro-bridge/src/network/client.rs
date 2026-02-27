use std::collections::HashMap;
use std::error::Error;
use std::net::UdpSocket;
use std::sync::{Arc, Mutex, RwLock};
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};
use renet::{ConnectionConfig, RenetClient};
use renet_netcode::{ClientAuthentication, ConnectToken, NetcodeClientTransport};
use crate::util::image::NativeImage;

static INSTANCE: Mutex<Option<Arc<RetroClient>>> = Mutex::new(None);

pub struct RetroClient {
    client: Mutex<(RenetClient, NetcodeClientTransport)>,
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
            client: Mutex::new((RenetClient::new(ConnectionConfig::default()), transport)),
            displays: RwLock::new(Default::default()),

            shutdown_requested: AtomicBool::new(false),
        }
    }
}

// TODO: uint32_t* RetroClient::registerDisplay(const jUUID *uuid, int width, int height, uint32_t *data, int sampleRate, int codec) {
// TODO: void RetroClient::unregisterDisplay(const jUUID* uuid) {
// TODO: std::shared_ptr<NativeImage> RetroClient::getDisplay(const jUUID *uuid) {
// TODO: void RetroClient::sendControlsUpdate(const jUUID *link, const int port, const int16_t controls) {
// TODO: void RetroClient::mainLoop() {
// TODO: void RetroClient::bandwidthMonitorLoop() {
// TODO: void RetroClient::onConnect() {
// TODO: void RetroClient::onDisconnect() {
// TODO: void RetroClient::onMessage(const ENetPacket *packet) {
