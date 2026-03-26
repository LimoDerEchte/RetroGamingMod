use tracing::info;
use retro_bridge::network::server::RetroServer;
use retro_bridge::platform::generic_console::ConsoleRegistry;

fn main() {
    tracing_subscriber::fmt::init();
    info!("Running Test");

    RetroServer::init(10, "0.0.0.0:6789", [ "127.0.0.1:6789".to_string() ].to_vec()).unwrap();

    let id = ConsoleRegistry::register_new(160, 144, 0, 0);
    ConsoleRegistry::with_console(id, |console| {
        console.load("".to_string(), "".to_string(), "".to_string(), "".to_string()).unwrap();
        Ok(())
    }).unwrap();

    RetroServer::deinit().unwrap();
}
