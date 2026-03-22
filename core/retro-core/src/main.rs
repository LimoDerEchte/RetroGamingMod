mod platform;
mod util;

use std::env;
use shared_memory::ShmemConf;
use tracing::{warn};
use retro_shared::shared::shared_memory::SharedMemory;
use crate::platform::generic_console::GenericConsole;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt::init();

    let args: Vec<String> = env::args().collect();
    if args.len() != 6 {
        warn!("This should NEVER be called by a user (to few arguments)");
        warn!("Args: '{:?}'", args.join("' '"));
        return Err(Box::from("Invalid Arguments"))
    }

    let shared_memory = ShmemConf::new().os_id(args[2].clone()).open()?;

    let ptr = shared_memory.as_ptr() as *mut SharedMemory;
    let data = unsafe { &mut *ptr };

    GenericConsole::init(*data, &args[3], &args[4], &args[5])?;
    GenericConsole::run();

    Ok(())
}
