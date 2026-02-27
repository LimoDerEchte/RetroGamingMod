use std::error::Error;
use std::mem;
use rand::distr::{Alphanumeric, SampleString};
use retro_shared::shared::shared_memory::SharedMemory;
use std::process::{Child, Command};
use std::time::{Duration, Instant};
use shared_memory::ShmemConf;
use tracing::{info, warn};
use wait_timeout::ChildExt;
use crate::codec::audio_encoder::{AudioEncoder, AudioEncoderOpus};
use crate::codec::video_encoder::{VideoEncoder, VideoEncoderAV1};

pub struct GenericConsole {
    audio_encoder: Box<dyn AudioEncoder>,
    video_encoder: Box<dyn VideoEncoder>,

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
            audio_encoder: Box::new(match audio_codec {
                0 => AudioEncoderOpus::new(),
                _ => {
                    warn!("Invalid audio codec supplied (ID {:?}). Falling back to Opus", audio_codec);
                    AudioEncoderOpus::new()
                }
            }),
            video_encoder: Box::new(match video_codec {
                0 => VideoEncoderAV1::new(width as u32, height as u32),
                _ => {
                    warn!("Invalid video codec supplied (ID {:?}). Falling back to AV1", video_codec);
                    VideoEncoderAV1::new(width as u32, height as u32)
                }
            }),

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
        (*self.video_encoder).retrieve_packet()
    }

    pub fn encode_audio_packet(&mut self) -> Option<Vec<u8>> {
        (*self.audio_encoder).encode_frame(self.shared_data.audio_data.to_vec()).ok()
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

// TODO: Audio / Video submission loop

// TODO: void GenericConsoleRegistry::registerConsole(GenericConsole *console) {
// TODO: void GenericConsoleRegistry::unregisterConsole(GenericConsole *console) {
// TODO: void GenericConsoleRegistry::withConsoles(const bool writing, const std::function<void(GenericConsole*)>& func) {
// TODO: void GenericConsoleRegistry::withConsole(const bool writing, const jUUID *uuid, const std::function<void(GenericConsole*)> &func) {
