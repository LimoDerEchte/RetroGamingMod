use std::collections::HashMap;
use std::error::Error;
use rand::distr::{Alphanumeric, SampleString};
use retro_shared::shared::shared_memory::SharedMemory;
use std::process::{Child, Command};
use std::sync::{Mutex, RwLock};
use std::time::{Duration};
use dav1d::Error::InvalidArgument;
use shared_memory::ShmemConf;
use tracing::{info, warn};
use wait_timeout::ChildExt;
use crate::codec::audio_encoder::{AudioEncoder, AudioEncoderOpus};
use crate::codec::video_encoder::{VideoEncoder, VideoEncoderAV1};

pub struct GenericConsole {
    id: i32,

    audio_encoder: Mutex<Box<dyn AudioEncoder>>,
    video_encoder: Mutex<Box<dyn VideoEncoder>>,

    shared_id: String,
    shared_data: SharedMemory,
    core_process: Option<Child>,
}

impl GenericConsole {
    pub fn new(width: i32, height: i32, id: i32, video_codec: i32, audio_codec: i32) -> Self {
        let mem_id = Alphanumeric.sample_string(&mut rand::rng(), 64);
        let shared_memory = ShmemConf::new()
            .os_id(mem_id.clone())
            .size(size_of::<SharedMemory>())
            .create()
            .expect("Failed to create shared memory");

        Self {
            id,

            audio_encoder: Mutex::new(Box::new(match audio_codec {
                0 => AudioEncoderOpus::new(),
                _ => {
                    warn!("Invalid audio codec supplied (ID {:?}). Falling back to Opus", audio_codec);
                    AudioEncoderOpus::new()
                }
            })),
            video_encoder: Mutex::new(Box::new(match video_codec {
                0 => VideoEncoderAV1::new(width as u32, height as u32),
                _ => {
                    warn!("Invalid video codec supplied (ID {:?}). Falling back to AV1", video_codec);
                    VideoEncoderAV1::new(width as u32, height as u32)
                }
            })),

            shared_id: mem_id,
            shared_data: unsafe { *(shared_memory.as_ptr() as *mut SharedMemory) },
            core_process: None,
        }
    }

    pub fn load(&mut self, retro_core: String, core: String, rom: String, save: String) -> Result<(), Box<dyn Error>> {
        self.core_process = Some(
            Command::new(retro_core)
                .arg("gn")
                .arg(self.shared_id.as_str())
                .arg(core.as_str())
                .arg(rom.as_str())
                .arg(save.as_str())
                .spawn()?
        );
        Ok(())
    }

    pub fn retrieve_video_packet(&mut self) -> Option<Vec<u8>> {
        (*self.video_encoder.get_mut().unwrap()).retrieve_packet()
    }

    pub fn encode_audio_packet(&mut self) -> Option<Vec<u8>> {
        (*self.audio_encoder.get_mut().unwrap()).encode_frame(self.shared_data.audio_data.to_vec()).ok()
    }

    pub fn submit_input(&mut self, port: i32, data: i16) {
        self.shared_data.controls[port as usize] = data as u16
    }
}

impl Drop for GenericConsole {
    fn drop(&mut self) {
        self.shared_data.shutdown_requested = true;
        if let Some(mut core_process) = self.core_process.take() {
            match core_process.wait_timeout(Duration::from_secs(10)) {
                Ok(Some(status)) => {
                    info!("Core process exited with status {:?}", status);
                }
                _ => {
                    core_process.kill().expect("Failed to kill child");
                    core_process.wait().expect("Failed to wait after kill");
                    warn!("Core process had to be killed after 10 seconds!")
                }
            }
        }
    }
}

static REGISTRY: RwLock<Option<ConsoleRegistry>> = RwLock::new(None);

#[derive(Default)]
pub struct ConsoleRegistry {
    incrementor: i32,
    registry: HashMap<i32, RwLock<GenericConsole>>
}

impl ConsoleRegistry {
    fn with_instance<T>(func: impl FnOnce(&ConsoleRegistry) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        let guard = REGISTRY.read()?;
        func(guard.as_ref().unwrap())
    }

    fn with_instance_mut<T>(func: impl FnOnce(&mut ConsoleRegistry) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        let mut guard = REGISTRY.write()?;
        if guard.is_none() {
            *guard = Some(ConsoleRegistry::default());
        }
        func(guard.as_mut().unwrap())
    }

    pub fn register_new(width: i32, height: i32, video_codec: i32, audio_codec: i32) -> i32 {
        Self::with_instance_mut(|instance| {
            let id = instance.incrementor;
            instance.incrementor += 1;

            instance.registry.insert(id, RwLock::new(GenericConsole::new(width, height, id, video_codec, audio_codec)));
            Ok(id)
        }).unwrap_or(-1)
    }

    pub fn unregister(id: i32) {
        Self::with_instance_mut(|instance| {
            instance.registry.remove(&id);
            Ok(())
        }).unwrap();
    }

    pub fn with_console<T>(id: i32, func: impl FnOnce(&GenericConsole) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        Self::with_instance(|instance| {
            if let Some(console) = instance.registry.get(&id) {
                let guard = console.read().unwrap();
                return func(&guard);
            }
            Err(Box::new(InvalidArgument))
        })
    }

    pub fn with_console_mut<T>(id: i32, func: impl FnOnce(&mut GenericConsole) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        Self::with_instance(|instance| {
            if let Some(console) = instance.registry.get(&id) {
                let mut guard = console.write().unwrap();
                return func(&mut guard);
            }
            Err(Box::new(InvalidArgument))
        })
    }

    pub fn foreach(func: impl Fn(&GenericConsole)) {
        Self::with_instance(|instance| {
            instance.registry.values().for_each(|reg| {
                func(&reg.read().unwrap());
            });
            Ok(())
        }).unwrap();
    }

    pub fn foreach_mut(func: impl Fn(&mut GenericConsole)) {
        Self::with_instance(|instance| {
            instance.registry.values().for_each(|reg| {
                func(&mut reg.write().unwrap());
            });
            Ok(())
        }).unwrap();
    }
}

// TODO: Audio / Video submission loop
