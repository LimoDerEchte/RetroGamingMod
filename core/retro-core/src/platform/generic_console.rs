use crate::util::libretro::LibRetroCore;
use crate::util::reader::convert_data;
use retro_shared::shared::shared_memory::SharedMemory;
use std::collections::VecDeque;
use std::path::Path;
use std::process::exit;
use std::ptr;
use std::sync::RwLock;
use std::time::{Duration, Instant};

static INSTANCE: RwLock<Option<GenericConsole>> = RwLock::new(None);

fn get_directory(file_path: &str) -> Option<String> {
    let path = Path::new(file_path);
    path.parent().map(|p| p.to_string_lossy().into_owned())
}

pub struct GenericConsole {
    shared: SharedMemory,
    audio_buffer: VecDeque<u16>
}

impl GenericConsole {
    const SAVE_DELAY_SECONDS: u64 = 30;

    pub fn init(data: SharedMemory, core: &str, rom: &str, save: &str) -> Result<(), Box<dyn std::error::Error>> {
        LibRetroCore::construct_instance(core, get_directory(core).unwrap().as_str(), rom, save)?;

        let mut guard = INSTANCE.write()?;
        *guard = Some(Self {
            shared: data,
            audio_buffer: Default::default(),
        });

        LibRetroCore::set_video_callback(|fmt, data, width, height, pitch| {
            let mut guard = INSTANCE.write().unwrap();
            let instance = guard.as_mut().unwrap();

            let slice: &mut [u32] = &mut instance.shared.display_data;
            convert_data(fmt, data, width, height, pitch, slice);

            instance.shared.display_changed = true;
        });

        LibRetroCore::set_audio_callback(|data, pitch| {
            let mut guard = INSTANCE.write().unwrap();
            let instance = guard.as_mut().unwrap();

            let samples = pitch * 2;
            for i in 0..samples {
                instance.audio_buffer.push_back(unsafe {
                    ptr::read_unaligned(data.add(i * 2))
                })
            }

            while instance.audio_buffer.len() > SharedMemory::AUDIO_FRAME_SIZE {
                for i in 0..SharedMemory::AUDIO_FRAME_SIZE {
                    instance.shared.audio_data[i] = instance.audio_buffer.pop_front().unwrap()
                }
                instance.shared.audio_changed = true;
            }
            pitch
        });

        LibRetroCore::set_input_callback(|port, id| {
            let guard = INSTANCE.read().unwrap();
            let instance = guard.as_ref().unwrap();

            match instance.shared.controls[port as usize] & 1 << id {
                1 => 0x7FFF,
                _ => 0,
            }
        });

        LibRetroCore::init()?;
        Ok(())
    }

    pub fn run() {
        std::thread::spawn(LibRetroCore::run_core);
        Self::core_loop()
    }

    fn core_loop() {
        let save_delay = Duration::from_secs(Self::SAVE_DELAY_SECONDS);
        let wait_delay = Duration::from_millis(500);
        let mut next = Instant::now() + save_delay;

        loop {
            let now = Instant::now();
            if now >= next {
                LibRetroCore::save_game();
                next += save_delay;
            }

            {
                let guard = INSTANCE.read().unwrap();
                let instance = guard.as_ref().unwrap();
                if instance.shared.shutdown_requested {
                    break;
                }
            }

            std::thread::sleep(wait_delay);
        }

        LibRetroCore::save_game();
        LibRetroCore::deinit();
        exit(0);
    }
}
