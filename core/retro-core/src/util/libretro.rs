use libloading::Library;
use rust_libretro_sys::{retro_game_info, retro_system_av_info, retro_system_info};

pub struct LibRetroCore {
    core: Library,

    retro_init: Option<unsafe extern "C" fn()>,
    retro_deinit: Option<unsafe extern "C" fn()>,
    retro_run: Option<unsafe extern "C" fn()>,

    retro_load_game: Option<unsafe extern "C" fn(game: *const retro_game_info) -> bool>,
    retro_unload_game: Option<unsafe extern "C" fn()>,
    retro_get_memory_data: Option<unsafe extern "C" fn(id: u32) -> *const std::ffi::c_void,>,
    retro_get_memory_size: Option<unsafe extern "C" fn(id: u32) -> usize>,

    retro_set_input_poll: Option<unsafe extern "C" fn(data: *const std::ffi::c_void)>,
    retro_set_input_state: Option<unsafe extern "C" fn(port: u32, device: u32, index: u32, id: u32)>,
    retro_set_audio_sample: Option<unsafe extern "C" fn(left: i16, right: i16)>,
    retro_set_audio_sample_batch: Option<unsafe extern "C" fn(data: *const i16, frames: usize)>,
    retro_set_video_refresh: Option<unsafe extern "C" fn(data: *const std::ffi::c_void, width: u32, height: u32, pitch: usize)>,

    retro_get_system_info: Option<unsafe extern "C" fn(info: *mut retro_system_info)>,
    retro_get_system_av_info: Option<unsafe extern "C" fn(av_info: *mut retro_system_av_info)>,
    retro_set_environment: Option<unsafe extern "C" fn(cmd: u32, data: *const std::ffi::c_void)>,

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

            core_path: core_path.to_string(),
            system_path: system_path.to_string(),
            rom_path: rom_path.to_string(),
        })
    }

    pub fn init(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        unsafe {
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
        }
        Ok(())
    }
}
