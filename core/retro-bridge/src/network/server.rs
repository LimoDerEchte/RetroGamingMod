use crate::network::network_definitions::RETRO_PROTOCOL;
use rand::TryRng;
use renet::{ConnectionConfig, RenetServer};
use renet_netcode::{NetcodeServerTransport, ServerAuthentication, ServerConfig};
use std::error::Error;
use std::net::{SocketAddr, UdpSocket};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{SystemTime, UNIX_EPOCH};

static INSTANCE: RwLock<Option<Arc<RwLock<RetroServer>>>> = RwLock::new(None);

pub struct RetroServer {
    server: Mutex<RenetServer>,
    transport: Mutex<NetcodeServerTransport>,

    shutdown_requested: AtomicBool,
}

impl RetroServer {
    pub fn with_instance<T>(func: impl FnOnce(&mut RetroServer) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        let mut guard = INSTANCE.write()?;
        if let Some(instance) = guard.as_mut() {
            return func(instance.write().as_mut().unwrap());
        }
        Err("Instance not found!".into())
    }

    pub fn init(max_users: i16, bind: &str, addresses: Vec<String>) -> Result<(), Box<dyn Error>> {
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

    fn new(max_users: i16, bind: &str, addresses: Vec<String>) -> Result<Self, Box<dyn Error>> {
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
            public_addresses,
            authentication: ServerAuthentication::Secure { private_key },
        };

        Ok(Self {
            server: Mutex::new(RenetServer::new(ConnectionConfig::default())),
            transport: Mutex::new(NetcodeServerTransport::new(config, socket)?),

            shutdown_requested: AtomicBool::new(false),
        })
    }
}

// TODO: char* RetroServer::genToken() {
// TODO: void RetroServer::mainReceiverLoop() {
// TODO: void RetroServer::bandwidthMonitorLoop() {
// TODO: void RetroServer::mainKeepAliveLoop() {
// TODO: void RetroServer::videoSenderLoop(const int fps) {
// TODO: void RetroServer::audioSenderLoop(const int cps) {
// TODO: void RetroServer::onConnect(ENetPeer *peer) {
// TODO: void RetroServer::onDisconnect(ENetPeer *peer) {
// TODO: void RetroServer::onMessage(ENetPeer *peer, const ENetPacket *packet) {
// TODO: void RetroServer::kick(ENetPeer *peer, const char *message) {
// TODO: std::shared_ptr<RetroServerClient> RetroServer::findClientByPeer(const ENetPeer* peer) const {
