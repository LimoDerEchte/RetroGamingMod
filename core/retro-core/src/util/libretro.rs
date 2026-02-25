use std::ffi::{c_void, CString};
use std::fs::File;
use std::io::Read;
use std::mem::MaybeUninit;
use std::ptr::null;
use libloading::Library;
use rust_libretro_sys::{retro_game_info, retro_system_av_info, retro_system_info};
use tracing::info;

unsafe extern "C" fn set_environment_callback(cmd: u32, data: *const std::ffi::c_void) -> bool {
    // TODO
    true
}

unsafe extern "C" fn input_state_callback(port: u32, device: u32, index: u32, id: u32) -> i32 {
    // TODO
    0
}

unsafe extern "C" fn input_poll_callback() {
    // TODO
}

unsafe extern "C" fn video_refresh_callback(data: *const std::ffi::c_void, width: u32, height: u32, pitch: usize) {
    // TODO
}

unsafe extern "C" fn audio_sample_callback(left: i16, right: i16) {
    // TODO
}

unsafe extern "C" fn audio_sample_batch_callback(data: *const i16, frames: usize) -> usize {
    // TODO
    0
}

pub struct LibRetroCore {
    core: Library,

    retro_init: Option<unsafe extern "C" fn()>,
    retro_deinit: Option<unsafe extern "C" fn()>,
    retro_run: Option<unsafe extern "C" fn()>,

    retro_load_game: Option<unsafe extern "C" fn(game: *const retro_game_info) -> bool>,
    retro_unload_game: Option<unsafe extern "C" fn()>,
    retro_get_memory_data: Option<unsafe extern "C" fn(id: u32) -> *const std::ffi::c_void>,
    retro_get_memory_size: Option<unsafe extern "C" fn(id: u32) -> usize>,
    retro_get_system_info: Option<unsafe extern "C" fn(info: *mut retro_system_info)>,
    retro_get_system_av_info: Option<unsafe extern "C" fn(av_info: *mut retro_system_av_info)>,

    retro_set_input_poll: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn()>)>,
    retro_set_input_state: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(port: u32, device: u32, index: u32, id: u32) -> i32>)>,
    retro_set_audio_sample: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(left: i16, right: i16)>)>,
    retro_set_audio_sample_batch: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(data: *const i16, frames: usize) -> usize>)>,
    retro_set_video_refresh: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(data: *const std::ffi::c_void, width: u32, height: u32, pitch: usize)>)>,
    retro_set_environment: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(cmd: u32, data: *const std::ffi::c_void) -> bool>)>,

    system_info: MaybeUninit<retro_system_info>,
    system_av_info: MaybeUninit<retro_system_av_info>,

    core_path: String,
    system_path: String,
    rom_path: String,
}

impl LibRetroCore {
    pub fn new(core_path: &str, system_path: &str, rom_path: &str) -> Result<Self, Box<dyn std::error::Error>> {
        Ok(Self {
            core: unsafe{ Library::new(core_path)? },

            retro_init: None,
            retro_deinit: None,
            retro_run: None,
            retro_load_game: None,
            retro_unload_game: None,
            retro_get_memory_data: None,
            retro_get_memory_size: None,
            retro_set_input_poll: None,
            retro_set_input_state: None,
            retro_set_audio_sample: None,
            retro_set_audio_sample_batch: None,
            retro_set_video_refresh: None,
            retro_get_system_info: None,
            retro_get_system_av_info: None,
            retro_set_environment: None,

            system_info: MaybeUninit::uninit(),
            system_av_info: MaybeUninit::uninit(),

            core_path: core_path.to_string(),
            system_path: system_path.to_string(),
            rom_path: rom_path.to_string(),
        })
    }

    pub fn init(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        unsafe {
            // Load Symbols
            self.retro_init = Some(*self.core.get(b"retro_init\0")?);
            self.retro_deinit = Some(*self.core.get(b"retro_deinit\0")?);
            self.retro_run = Some(*self.core.get(b"retro_run\0")?);

            self.retro_load_game = Some(*self.core.get(b"retro_load_game\0")?);
            self.retro_unload_game = Some(*self.core.get(b"retro_unload_game\0")?);
            self.retro_get_memory_data = Some(*self.core.get(b"retro_get_memory_data\0")?);
            self.retro_get_memory_size = Some(*self.core.get(b"retro_get_memory_size\0")?);

            self.retro_set_input_poll = Some(*self.core.get(b"retro_set_input_poll\0")?);
            self.retro_set_input_state = Some(*self.core.get(b"retro_set_input_state\0")?);
            self.retro_set_audio_sample = Some(*self.core.get(b"retro_set_audio_sample\0")?);
            self.retro_set_audio_sample_batch = Some(*self.core.get(b"retro_set_audio_sample_batch\0")?);
            self.retro_set_video_refresh = Some(*self.core.get(b"retro_set_video_refresh\0")?);

            self.retro_get_system_info = Some(*self.core.get(b"retro_get_system_info\0")?);
            self.retro_get_system_av_info = Some(*self.core.get(b"retro_get_system_av_info\0")?);
            self.retro_set_environment = Some(*self.core.get(b"retro_set_environment\0")?);

            // Initialize Core
            self.retro_set_environment.unwrap()(Some(set_environment_callback));

            self.retro_get_system_info.unwrap()(self.system_info.as_mut_ptr());
            let info = self.system_info.assume_init();
            info!("Loaded core: {:?} v{:?}", info.library_name, info.library_version);

            self.retro_set_video_refresh.unwrap()(Some(video_refresh_callback));
            self.retro_set_input_poll.unwrap()(Some(input_poll_callback));
            self.retro_set_input_state.unwrap()(Some(input_state_callback));
            self.retro_set_audio_sample.unwrap()(Some(audio_sample_callback));
            self.retro_set_audio_sample_batch.unwrap()(Some(audio_sample_batch_callback));

            self.retro_init.unwrap()();

            // Load ROM
            let mut file = File::open(self.rom_path.clone())?;
            let mut buffer = Vec::new();
            file.read_to_end(&mut buffer)?;

            let c_path = CString::new(self.rom_path.clone())?;
            let data_ptr: *const c_void = buffer.as_ptr() as *const c_void;
            let data_size: usize = buffer.len();

            let game_info = retro_game_info {
                path: c_path.as_ptr(),
                data: data_ptr,
                size: data_size,
                meta: null(),
            };

            if !self.retro_load_game.unwrap()(&game_info) {
                return Err("Failed to load game info")?;
            }

            self.retro_get_system_av_info.unwrap()(self.system_av_info.as_mut_ptr());
            let info = self.system_av_info.assume_init();
            info!("Game successfully loaded.");
            info!("Display info: {:?}x{:?} @ {:?} fps", info.geometry.base_width, info.geometry.base_height, info.timing.fps);
        }
        Ok(())
    }
}
