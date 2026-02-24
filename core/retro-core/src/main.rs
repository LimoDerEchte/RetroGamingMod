mod platform;
mod util;

use tracing::info;

fn main() {
    tracing_subscriber::fmt::init();
    info!("Hello, world!");
}
