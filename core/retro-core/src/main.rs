mod platform;
mod util;

use std::{env, panic};
use std::panic::catch_unwind;
use shared_memory::ShmemConf;
use tracing::{error, warn};
use retro_shared::shared::shared_memory::SharedMemory;
use crate::platform::generic_console::GenericConsole;

fn main() {
    tracing_subscriber::fmt::init();

    panic::set_hook(Box::new(|info| {
        let location = info.location();
        let message = info.payload()
            .downcast_ref::<&str>()
            .copied()
            .or_else(|| info.payload().downcast_ref::<String>().map(|s| s.as_str()))
            .unwrap_or("unknown panic");

        error!(
            panic.message = message,
            panic.file = location.map(|l| l.file()),
            panic.line = location.map(|l| l.line()),
            "panic captured"
        );
    }));

    let _ = catch_unwind(inner_main);
}

fn inner_main() -> Result<(), Box<dyn std::error::Error>> {
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
    warn!("4");
    GenericConsole::run();

    Ok(())
}
