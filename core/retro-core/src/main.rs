mod platform;
mod util;

use std::env;
use shared_memory::ShmemConf;
use tracing::{info, warn};
use retro_shared::shared::shared_memory::SharedMemory;
use crate::platform::generic_console::generic_console_load;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt::init();

    let args: Vec<String> = env::args().collect();
    if args.len() != 5 {
        warn!("This should NEVER be called by a user (to few arguments)");
        warn!("Usage: retro-core <platform> <id> <core> <rom> <save>");
        return Err(Box::from("Test"))
    }

    let shared_memory = ShmemConf::new().os_id(args[1].clone()).open()?;

    let ptr = shared_memory.as_ptr() as *mut SharedMemory;
    let data = unsafe { &mut *ptr };

    generic_console_load(data, &args[2], &args[3], &args[4]);
    Ok(())
}
