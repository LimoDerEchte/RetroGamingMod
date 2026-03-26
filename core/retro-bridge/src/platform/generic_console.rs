use std::collections::HashMap;
use std::error::Error;
use rand::distr::{Alphanumeric, SampleString};
use retro_shared::shared::shared_memory::SharedMemory;
use std::process::{Child, Command};
use parking_lot::{Mutex, RwLock};
use std::sync::atomic::AtomicI32;
use std::sync::OnceLock;
use std::time::{Duration};
use dav1d::Error::InvalidArgument;
use shared_memory::{Shmem, ShmemConf};
use tracing::{info, warn};
use wait_timeout::ChildExt;
use crate::codec::audio_encoder::{AudioEncoder, AudioEncoderOpus};
use crate::codec::video_encoder::{VideoEncoder, VideoEncoderAV1};

#[allow(unused)]
pub struct GenericConsole {
    audio_encoder: Mutex<Box<dyn AudioEncoder>>,
    video_encoder: Mutex<Box<dyn VideoEncoder>>,

    shared_id: String,
    shared_segment: Shmem,
    shared_data: *mut SharedMemory,
    core_process: Option<Child>,
}

unsafe impl Send for GenericConsole {}
unsafe impl Sync for GenericConsole {}

impl GenericConsole {
    pub fn new(width: i32, height: i32, video_codec: i32, audio_codec: i32) -> Self {
        let mem_id = Alphanumeric.sample_string(&mut rand::rng(), 60);
        let shared_memory = ShmemConf::new()
            .os_id(mem_id.clone())
            .size(size_of::<SharedMemory>())
            .create()
            .expect("Failed to create shared memory");
        info!("Created shared memory: {:?}", mem_id);
        let data = shared_memory.as_ptr() as *mut SharedMemory;

        Self {
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
            shared_segment: shared_memory,
            shared_data: data,
            core_process: None,
        }
    }

    fn dispose(&mut self) {
        unsafe { &mut *self.shared_data }.shutdown_requested = true;
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

    pub fn retrieve_video_packet(&self) -> Option<Vec<u8>> {
        self.video_encoder.lock().retrieve_packet()
    }

    pub fn encode_video_frame(&self) {
        if unsafe { &*self.shared_data }.display_changed {
            self.video_encoder.lock().submit_frame(unsafe { &*self.shared_data }.display_data.to_vec())
        }
    }

    pub fn encode_audio_packet(&self) -> Option<Vec<u8>> {
        if !unsafe { &*self.shared_data }.audio_changed {
            return None;
        }
        self.audio_encoder.lock().encode_frame(unsafe { &*self.shared_data }.audio_data.to_vec()).ok()
    }

    pub fn submit_input(&mut self, port: i16, data: i16) {
        unsafe { &mut *self.shared_data }.controls[port as usize] = data as u16
    }
}

static REGISTRY: OnceLock<ConsoleRegistry> = OnceLock::new();

#[derive(Default)]
pub struct ConsoleRegistry {
    incrementor: AtomicI32,
    registry: RwLock<HashMap<i32, RwLock<GenericConsole>>>,
}

impl ConsoleRegistry {
    pub fn register_new(width: i32, height: i32, video_codec: i32, audio_codec: i32) -> i32 {
        let instance = REGISTRY.get_or_init(ConsoleRegistry::default);
        let id = instance.incrementor.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        instance.registry.write().insert(id, RwLock::new(GenericConsole::new(width, height, video_codec, audio_codec)));
        id
    }

    pub fn unregister(id: i32) {
        let instance = REGISTRY.get_or_init(ConsoleRegistry::default);
        if let Some(removed) = instance.registry.write().remove(&id) {
            std::thread::spawn(move || {
                removed.write().dispose();
            });
        }
    }

    pub fn with_console<T>(id: i32, func: impl FnOnce(&mut GenericConsole) -> Result<T, Box<dyn Error>>) -> Result<T, Box<dyn Error>> {
        let instance = REGISTRY.get_or_init(ConsoleRegistry::default);

        if let Some(console) = instance.registry.read().get(&id) {
            return func(&mut console.write());
        }
        Err(Box::new(InvalidArgument))
    }

    pub fn foreach(func: impl Fn(&GenericConsole)) {
        let instance = REGISTRY.get_or_init(ConsoleRegistry::default);

        instance.registry.read().values().for_each(|reg| {
            func(&reg.read());
        });
    }
}
