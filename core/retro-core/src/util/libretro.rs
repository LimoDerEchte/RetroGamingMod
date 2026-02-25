use std::collections::HashMap;
use std::ffi::{c_char, c_void, CStr, CString};
use std::fs::File;
use std::io::Read;
use std::mem::MaybeUninit;
use std::ptr::{null};
use std::sync::{OnceLock, RwLock};
use libloading::Library;
use rust_libretro_sys::*;
use rust_libretro_sys::retro_pixel_format::{RETRO_PIXEL_FORMAT_RGB565, RETRO_PIXEL_FORMAT_XRGB8888};
use tracing::{info, warn};

static INSTANCE: RwLock<Option<LibRetroCore>> = RwLock::new(None);
static CONTENT_DIR: OnceLock<CString> = OnceLock::new();

#[allow(dead_code, unused_assignments)]
unsafe extern "C" fn set_environment_callback(cmd: u32, data: *const c_void) -> bool {
    unsafe { match INSTANCE.write().unwrap().as_mut() {
        Some(instance) => {
            match cmd {
                RETRO_ENVIRONMENT_GET_LOG_INTERFACE => {
                    let mut callback = data as *mut retro_log_callback;
                    // TODO: implement the actual callback using a c trampoline
                    true
                }
                RETRO_ENVIRONMENT_GET_CAN_DUPE => {
                    *(data as *mut u8) = 1;
                    true
                }
                RETRO_ENVIRONMENT_SET_PIXEL_FORMAT => {
                    let format = *(data as *const retro_pixel_format);
                    if format as i32 > RETRO_PIXEL_FORMAT_RGB565 as i32 {
                        warn!("Core tried to use unsupported pixel format: {:?}", format);
                        false
                    } else {
                        instance.pixel_format = format;
                        true
                    }
                }
                RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY | RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY | RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY => {
                    let out = data as *mut *const c_char;
                    match CONTENT_DIR.get() {
                        Some(path) => {
                            *out = path.as_ptr();
                            true
                        }
                        None => false,
                    }
                }
                RETRO_ENVIRONMENT_SET_VARIABLE => {
                    data != null()
                }
                RETRO_ENVIRONMENT_GET_VARIABLE => {
                    let mut var = *(data as *mut retro_variable);
                    let key = CStr::from_ptr(var.key).to_str().unwrap();

                    match instance.environment_variables.get(key) {
                        Some(value) => {
                            var.value = value.as_ptr();
                            true
                        }
                        None => {
                            warn!("Environment variable not found: {:?}", key);
                            var.value = null();
                            false
                        }
                    }
                }
                RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE => {
                    if data != null() {
                        *(data as *mut u8) = instance.environment_vars_updated;
                        if instance.environment_vars_updated == 1 {
                            instance.environment_vars_updated = 0;
                        }
                        true
                    } else {
                        false
                    }
                }
                _ => false
            }
        }
        None => false
    }}
}

#[allow(dead_code)]
unsafe extern "C" fn input_state_callback(port: u32, _: u32, _: u32, id: u32) -> u16 {
    match INSTANCE.read().unwrap().as_ref() {
        Some(instance) => {
            match instance.callback_input {
                Some(input) => {
                    input(port, id)
                }
                None => 0
            }
        }
        None => 0
    }
}

#[allow(dead_code)]
unsafe extern "C" fn input_poll_callback() {
    // Nothing to do - controls get updated by shared memory
}

#[allow(dead_code)]
unsafe extern "C" fn video_refresh_callback(data: *const c_void, width: u32, height: u32, pitch: usize) {
    match INSTANCE.read().unwrap().as_ref() {
        Some(instance) => {
            match instance.callback_video {
                Some(video) => {
                    video(instance.pixel_format, data as *const u8, width, height, pitch as u32);
                }
                None => {}
            }
        }
        None => {}
    }
}

#[allow(dead_code)]
unsafe extern "C" fn audio_sample_callback(_: i16, _: i16) {
    // This is usually unused by cores
}

#[allow(dead_code)]
unsafe extern "C" fn audio_sample_batch_callback(data: *const u16, frames: usize) -> usize {
    match INSTANCE.read().unwrap().as_ref() {
        Some(instance) => {
            match instance.callback_audio {
                Some(audio) => {
                    audio(data, frames)
                }
                None => 0
            }
        }
        None => 0
    }
}

pub struct LibRetroCore {
    core: Library,

    retro_init: Option<unsafe extern "C" fn()>,
    retro_deinit: Option<unsafe extern "C" fn()>,
    retro_run: Option<unsafe extern "C" fn()>,

    retro_load_game: Option<unsafe extern "C" fn(game: *const retro_game_info) -> bool>,
    retro_unload_game: Option<unsafe extern "C" fn()>,
    retro_get_memory_data: Option<unsafe extern "C" fn(id: u32) -> *const c_void>,
    retro_get_memory_size: Option<unsafe extern "C" fn(id: u32) -> usize>,
    retro_get_system_info: Option<unsafe extern "C" fn(info: *mut retro_system_info)>,
    retro_get_system_av_info: Option<unsafe extern "C" fn(av_info: *mut retro_system_av_info)>,

    retro_set_input_poll: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn()>)>,
    retro_set_input_state: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(port: u32, device: u32, index: u32, id: u32) -> u16>)>,
    retro_set_audio_sample: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(left: i16, right: i16)>)>,
    retro_set_audio_sample_batch: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(data: *const u16, frames: usize) -> usize>)>,
    retro_set_video_refresh: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(data: *const c_void, width: u32, height: u32, pitch: usize)>)>,
    retro_set_environment: Option<unsafe extern "C" fn(callback: Option<unsafe extern "C" fn(cmd: u32, data: *const c_void) -> bool>)>,

    core_path: String,
    system_path: String,
    rom_path: String,

    pixel_format: retro_pixel_format,
    environment_variables: HashMap<String, CString>,
    environment_vars_updated: u8,

    callback_video: Option<fn(fmt: retro_pixel_format, data: *const u8, width: u32, height: u32, pitch: u32)>,
    callback_audio: Option<fn(data: *const u16, frames: usize) -> usize>,
    callback_input: Option<fn(port: u32, id: u32) -> u16>,
}

impl LibRetroCore {
    pub fn construct_instance(core_path: &str, system_path: &str, rom_path: &str) -> Result<(), Box<dyn std::error::Error>> {
        INSTANCE.write()?.replace(Self {
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

            pixel_format: RETRO_PIXEL_FORMAT_XRGB8888,
            environment_variables: Default::default(),
            environment_vars_updated: 0,

            callback_video: None,
            callback_audio: None,
            callback_input: None,
        });
        Ok(())
    }

    pub fn set_video_callback(callback: fn(fmt: retro_pixel_format, data: *const u8, width: u32, height: u32, pitch: u32)) {
        match INSTANCE.write().unwrap().as_mut() {
            Some(instance) => {
                instance.callback_video = Some(callback);
            }
            None => {}
        }
    }

    pub fn set_audio_callback(callback: fn(data: *const u16, frames: usize) -> usize) {
        match INSTANCE.write().unwrap().as_mut() {
            Some(instance) => {
                instance.callback_audio = Some(callback);
            }
            None => {}
        }
    }

    pub fn set_input_callback(callback: fn(port: u32, id: u32) -> u16) {
        match INSTANCE.write().unwrap().as_mut() {
            Some(instance) => {
                instance.callback_input = Some(callback);
            }
            None => {}
        }
    }

    pub fn init() -> Result<(), Box<dyn std::error::Error>> {
        match INSTANCE.write().unwrap().as_mut() {
            Some(instance) => {
                unsafe {
                    // Load Symbols
                    instance.retro_init = Some(*instance.core.get(b"retro_init\0")?);
                    instance.retro_deinit = Some(*instance.core.get(b"retro_deinit\0")?);
                    instance.retro_run = Some(*instance.core.get(b"retro_run\0")?);

                    instance.retro_load_game = Some(*instance.core.get(b"retro_load_game\0")?);
                    instance.retro_unload_game = Some(*instance.core.get(b"retro_unload_game\0")?);
                    instance.retro_get_memory_data = Some(*instance.core.get(b"retro_get_memory_data\0")?);
                    instance.retro_get_memory_size = Some(*instance.core.get(b"retro_get_memory_size\0")?);

                    instance.retro_set_input_poll = Some(*instance.core.get(b"retro_set_input_poll\0")?);
                    instance.retro_set_input_state = Some(*instance.core.get(b"retro_set_input_state\0")?);
                    instance.retro_set_audio_sample = Some(*instance.core.get(b"retro_set_audio_sample\0")?);
                    instance.retro_set_audio_sample_batch = Some(*instance.core.get(b"retro_set_audio_sample_batch\0")?);
                    instance.retro_set_video_refresh = Some(*instance.core.get(b"retro_set_video_refresh\0")?);

                    instance.retro_get_system_info = Some(*instance.core.get(b"retro_get_system_info\0")?);
                    instance.retro_get_system_av_info = Some(*instance.core.get(b"retro_get_system_av_info\0")?);
                    instance.retro_set_environment = Some(*instance.core.get(b"retro_set_environment\0")?);

                    // Initialize Core
                    CONTENT_DIR.set(CString::new(instance.system_path.clone())?).unwrap();
                    instance.retro_set_environment.unwrap()(Some(set_environment_callback));

                    let mut system_info: MaybeUninit<retro_system_info> = MaybeUninit::uninit();
                    instance.retro_get_system_info.unwrap()(system_info.as_mut_ptr());

                    let info = system_info.assume_init();
                    info!("Loaded core: {:?} v{:?}", info.library_name, info.library_version);

                    instance.retro_set_video_refresh.unwrap()(Some(video_refresh_callback));
                    instance.retro_set_input_poll.unwrap()(Some(input_poll_callback));
                    instance.retro_set_input_state.unwrap()(Some(input_state_callback));
                    instance.retro_set_audio_sample.unwrap()(Some(audio_sample_callback));
                    instance.retro_set_audio_sample_batch.unwrap()(Some(audio_sample_batch_callback));

                    instance.retro_init.unwrap()();

                    // Load ROM
                    let mut file = File::open(instance.rom_path.clone())?;
                    let mut buffer = Vec::new();
                    file.read_to_end(&mut buffer)?;

                    let c_path = CString::new(instance.rom_path.clone())?;
                    let data_ptr: *const c_void = buffer.as_ptr() as *const c_void;
                    let data_size: usize = buffer.len();

                    let game_info = retro_game_info {
                        path: c_path.as_ptr(),
                        data: data_ptr,
                        size: data_size,
                        meta: null(),
                    };

                    if !instance.retro_load_game.unwrap()(&game_info) {
                        return Err("Failed to load game info")?;
                    }

                    let mut system_av_info: MaybeUninit<retro_system_av_info> = MaybeUninit::uninit();
                    instance.retro_get_system_av_info.unwrap()(system_av_info.as_mut_ptr());

                    let info = system_av_info.assume_init();
                    info!("Game successfully loaded.");
                    info!("Display info: {:?}x{:?} @ {:?} fps", info.geometry.base_width, info.geometry.base_height, info.timing.fps);

                    Ok(())
                }
            },
            None => Err("Instance missing".into())
        }
    }
}

impl Drop for LibRetroCore {
    fn drop(&mut self) {
        unsafe{
            match self.retro_unload_game {
                Some(unload_game) => { unload_game(); }
                None => {}
            }
            match self.retro_deinit {
                Some(deinit) => { deinit(); }
                None => {}
            }
        }
    }
}
