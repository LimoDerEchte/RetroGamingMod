use std::cmp::min;
use std::collections::HashMap;
use std::ffi::{c_char, c_void, CStr, CString};
use std::fs::File;
use std::io::{Read, Write};
use std::mem::MaybeUninit;
use std::{ptr, slice};
use std::sync::{OnceLock};
use std::time::{Duration, Instant};
use libloading::Library;
use parking_lot::RwLock;
use rust_libretro_sys::*;
use rust_libretro_sys::retro_pixel_format::{RETRO_PIXEL_FORMAT_RGB565, RETRO_PIXEL_FORMAT_XRGB8888};
use tracing::{debug, error, info, warn};

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct RetroGameInfo {
    pub path: *const std::os::raw::c_char,
    pub data: *const c_void,
    pub size: usize,
    pub meta: *const std::os::raw::c_char,
}

static INSTANCE: RwLock<Option<LibRetroCore>> = RwLock::new(None);
static CONTENT_DIR: OnceLock<CString> = OnceLock::new();

static RETRO_INIT: OnceLock<unsafe extern "C" fn()> = OnceLock::new();
static RETRO_DEINIT: OnceLock<unsafe extern "C" fn()> = OnceLock::new();
static RETRO_RUN: OnceLock<unsafe extern "C" fn()> = OnceLock::new();

static RETRO_LOAD_GAME: OnceLock<unsafe extern "C" fn(game: *const RetroGameInfo) -> bool> = OnceLock::new();
static RETRO_UNLOAD_GAME: OnceLock<unsafe extern "C" fn()> = OnceLock::new();
static RETRO_GET_MEMORY_DATA: OnceLock<unsafe extern "C" fn(id: u32) -> *const c_void> = OnceLock::new();
static RETRO_GET_MEMORY_SIZE: OnceLock<unsafe extern "C" fn(id: u32) -> usize> = OnceLock::new();
static RETRO_GET_SYSTEM_INFO: OnceLock<unsafe extern "C" fn(info: *mut retro_system_info)> = OnceLock::new();
static RETRO_GET_SYSTEM_AV_INFO: OnceLock<unsafe extern "C" fn(av_info: *mut retro_system_av_info)> = OnceLock::new();

static RETRO_SET_INPUT_POLL: OnceLock<unsafe extern "C" fn(callback: retro_input_poll_t)> = OnceLock::new();
static RETRO_SET_INPUT_STATE: OnceLock<unsafe extern "C" fn(callback: retro_input_state_t)> = OnceLock::new();
static RETRO_SET_AUDIO_SAMPLE: OnceLock<unsafe extern "C" fn(callback: retro_audio_sample_t)> = OnceLock::new();
static RETRO_SET_AUDIO_SAMPLE_BATCH: OnceLock<unsafe extern "C" fn(callback: retro_audio_sample_batch_t)> = OnceLock::new();
static RETRO_SET_VIDEO_REFRESH: OnceLock<unsafe extern "C" fn(callback: retro_video_refresh_t)> = OnceLock::new();
static RETRO_SET_ENVIRONMENT: OnceLock<unsafe extern "C" fn(callback: retro_environment_t)> = OnceLock::new();

#[unsafe(no_mangle)]
pub unsafe extern "C" fn rust_log_trampoline_receiver(level: retro_log_level, msg: *const c_char) {
    let message = unsafe{ CStr::from_ptr(msg) }.to_string_lossy().to_string();
    match level {
        retro_log_level::RETRO_LOG_DEBUG => debug!(message),
        retro_log_level::RETRO_LOG_INFO => info!(message),
        retro_log_level::RETRO_LOG_WARN => warn!(message),
        _ => error!(message),
    }
}

unsafe extern "C" {
    pub fn log_printf_trampoline(level: retro_log_level, fmt: *const c_char, ...);
}

#[allow(dead_code, unused_assignments)]
unsafe extern "C" fn set_environment_callback(cmd: u32, data: *mut c_void) -> bool {
    unsafe {
        match cmd {
            RETRO_ENVIRONMENT_GET_LOG_INTERFACE => {
                // TODO: Create impl that doesn't crash
                //let callback = data as *mut retro_log_callback;
                //(*callback).log = Some(log_printf_trampoline);
                //true
                false
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
                    INSTANCE.write().as_mut().unwrap().pixel_format = format;
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
                !data.is_null()
            }
            RETRO_ENVIRONMENT_GET_VARIABLE => {
                let mut var = *(data as *mut retro_variable);
                let key = CStr::from_ptr(var.key).to_str().unwrap();

                match INSTANCE.read().as_ref().unwrap().environment_variables.get(key) {
                    Some(value) => {
                        var.value = value.as_ptr();
                        true
                    }
                    None => {
                        warn!("Environment variable not found: {:?}", key);
                        var.value = ptr::null();
                        false
                    }
                }
            }
            RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE => {
                if !data.is_null() {
                    let mut guard = INSTANCE.write();
                    let instance = guard.as_mut().unwrap();

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
}

#[allow(dead_code)]
unsafe extern "C" fn input_state_callback(port: u32, _: u32, _: u32, id: u32) -> i16 {
    match INSTANCE.read().as_ref() {
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
    if let Some(instance) = INSTANCE.read().as_ref()
            && let Some(video) = instance.callback_video {
        video(instance.pixel_format, data as *const u8, width, height, pitch as u32);
    }
}

#[allow(dead_code)]
unsafe extern "C" fn audio_sample_callback(_: i16, _: i16) {
    // This is usually unused by cores
}

#[allow(dead_code)]
unsafe extern "C" fn audio_sample_batch_callback(data: *const i16, frames: usize) -> usize {
    match INSTANCE.read().as_ref() {
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

    system_path: String,
    rom_path: String,
    save_path: String,

    fps: f64,
    saving_supported: bool,
    pixel_format: retro_pixel_format,
    environment_variables: HashMap<String, CString>,
    environment_vars_updated: u8,

    callback_video: Option<fn(fmt: retro_pixel_format, data: *const u8, width: u32, height: u32, pitch: u32)>,
    callback_audio: Option<fn(data: *const i16, frames: usize) -> usize>,
    callback_input: Option<fn(port: u32, id: u32) -> i16>,
}

impl LibRetroCore {
    pub fn construct_instance(core_path: &str, system_path: &str, rom_path: &str, save_path: &str) -> Result<(), Box<dyn std::error::Error>> {
        INSTANCE.write().replace(Self {
            core: unsafe{ Library::new(core_path)? },

            system_path: system_path.to_string(),
            rom_path: rom_path.to_string(),
            save_path: save_path.to_string(),

            fps: 60.0,
            saving_supported: false,
            pixel_format: RETRO_PIXEL_FORMAT_XRGB8888,
            environment_variables: Default::default(),
            environment_vars_updated: 0,

            callback_video: None,
            callback_audio: None,
            callback_input: None,
        });
        Ok(())
    }

    pub fn run_core() {
        let fps = { INSTANCE.read().as_ref().unwrap().fps };
        let frame_time = Duration::from_micros((1000000.0 / fps) as u64);
        info!("Starting main loop at {:?} fps", fps);

        let mut next = Instant::now();
        loop {
            unsafe { RETRO_RUN.wait()() };

            next += frame_time;
            let now = Instant::now();
            if now < next {
                std::thread::sleep(next - now);
            } else {
                next = now;
                warn!("Main loop lagging behind!")
            }
        }
    }

    pub fn set_video_callback(callback: fn(fmt: retro_pixel_format, data: *const u8, width: u32, height: u32, pitch: u32)) {
        if let Some(instance) = INSTANCE.write().as_mut() {
            instance.callback_video = Some(callback);
        }
    }

    pub fn set_audio_callback(callback: fn(data: *const i16, frames: usize) -> usize) {
        if let Some(instance) = INSTANCE.write().as_mut() {
            instance.callback_audio = Some(callback);
        }
    }

    pub fn set_input_callback(callback: fn(port: u32, id: u32) -> i16) {
        if let Some(instance) = INSTANCE.write().as_mut() {
            instance.callback_input = Some(callback);
        }
    }

    pub fn init() -> Result<(), Box<dyn std::error::Error>> {
        unsafe {
            let mut opt = INSTANCE.write();
            let instance = opt.as_mut().unwrap();

            RETRO_INIT.set(*instance.core.get(b"retro_init\0")?).unwrap();
            RETRO_DEINIT.set(*instance.core.get(b"retro_deinit\0")?).unwrap();
            RETRO_RUN.set(*instance.core.get(b"retro_run\0")?).unwrap();

            RETRO_LOAD_GAME.set(*instance.core.get(b"retro_load_game\0")?).unwrap();
            RETRO_UNLOAD_GAME.set(*instance.core.get(b"retro_unload_game\0")?).unwrap();
            RETRO_GET_MEMORY_DATA.set(*instance.core.get(b"retro_get_memory_data\0")?).unwrap();
            RETRO_GET_MEMORY_SIZE.set(*instance.core.get(b"retro_get_memory_size\0")?).unwrap();

            RETRO_SET_INPUT_POLL.set(*instance.core.get(b"retro_set_input_poll\0")?).unwrap();
            RETRO_SET_INPUT_STATE.set(*instance.core.get(b"retro_set_input_state\0")?).unwrap();
            RETRO_SET_AUDIO_SAMPLE.set(*instance.core.get(b"retro_set_audio_sample\0")?).unwrap();
            RETRO_SET_AUDIO_SAMPLE_BATCH.set(*instance.core.get(b"retro_set_audio_sample_batch\0")?).unwrap();
            RETRO_SET_VIDEO_REFRESH.set(*instance.core.get(b"retro_set_video_refresh\0")?).unwrap();

            RETRO_GET_SYSTEM_INFO.set(*instance.core.get(b"retro_get_system_info\0")?).unwrap();
            RETRO_GET_SYSTEM_AV_INFO.set(*instance.core.get(b"retro_get_system_av_info\0")?).unwrap();
            RETRO_SET_ENVIRONMENT.set(*instance.core.get(b"retro_set_environment\0")?).unwrap();

            CONTENT_DIR.set(CString::new(instance.system_path.clone())?).unwrap();
        }

        unsafe {
            // Initialize Core
            RETRO_SET_ENVIRONMENT.wait()(Some(set_environment_callback));

            let mut system_info: MaybeUninit<retro_system_info> = MaybeUninit::uninit();
            RETRO_GET_SYSTEM_INFO.wait()(system_info.as_mut_ptr());

            let info = system_info.assume_init();
            info!("Loaded core: {:?} v{:?}", CStr::from_ptr(info.library_name), CStr::from_ptr(info.library_version));

            RETRO_SET_VIDEO_REFRESH.wait()(Some(video_refresh_callback));
            RETRO_SET_INPUT_POLL.wait()(Some(input_poll_callback));
            RETRO_SET_INPUT_STATE.wait()(Some(input_state_callback));
            RETRO_SET_AUDIO_SAMPLE.wait()(Some(audio_sample_callback));
            RETRO_SET_AUDIO_SAMPLE_BATCH.wait()(Some(audio_sample_batch_callback));

            RETRO_INIT.wait()();

            // Load ROM
            let rom_path = INSTANCE.read().as_ref().unwrap().rom_path.clone();

            let mut file = File::open(rom_path.clone())?;
            let mut buffer = Vec::new();
            file.read_to_end(&mut buffer)?;

            let c_path = CString::new(rom_path.clone())?;
            let data_ptr: *const c_void = buffer.as_ptr() as *const c_void;
            let data_size: usize = buffer.len();

            let game_info = RetroGameInfo {
                path: c_path.as_ptr(),
                data: data_ptr,
                size: data_size,
                meta: ptr::null(),
            };

            if !RETRO_LOAD_GAME.wait()(&game_info) {
                return Err("Failed to load game info")?;
            }

            let mut system_av_info: MaybeUninit<retro_system_av_info> = MaybeUninit::uninit();
            RETRO_GET_SYSTEM_AV_INFO.wait()(system_av_info.as_mut_ptr());

            let info = system_av_info.assume_init();
            info!("Game successfully loaded.");

            if info.timing.fps > 0.0 {
                info!("Display info: {:?}x{:?} @ {:?} fps", info.geometry.base_width, info.geometry.base_height, info.timing.fps);
                INSTANCE.write().as_mut().unwrap().fps = info.timing.fps;
            } else {
                info!("Display info: {:?}x{:?} @ 60 fps", info.geometry.base_width, info.geometry.base_height);
            }

            // Loading Save
            let save_data = RETRO_GET_MEMORY_DATA.wait()(RETRO_MEMORY_SAVE_RAM);
            let save_size = RETRO_GET_MEMORY_SIZE.wait()(RETRO_MEMORY_SAVE_RAM);

            if save_data.is_null() || save_size == 0 {
                warn!("Core does not support save RAM!");
                return Ok(())
            }

            let save_path = INSTANCE.read().as_ref().unwrap().rom_path.clone();
            match File::open(&save_path) {
                Ok(mut file) => {
                    let mut buffer = Vec::new();
                    file.read_to_end(&mut buffer)?;

                    let len = min(save_size, buffer.len());
                    ptr::copy_nonoverlapping(buffer.as_ptr(), save_data as *mut u8, len);
                }
                Err(_) => {
                    info!("No valid save file found.")
                }
            }
        }
        Ok(())
    }

    pub fn deinit() {
        unsafe {
            RETRO_UNLOAD_GAME.wait()();
            RETRO_DEINIT.wait()();
        }
    }

    pub fn save_game() {
        if let Some(instance) = INSTANCE.read().as_ref() {
            if !instance.saving_supported {
                return;
            }

            unsafe {
                let save_data = RETRO_GET_MEMORY_DATA.wait()(RETRO_MEMORY_SAVE_RAM);
                let save_size = RETRO_GET_MEMORY_SIZE.wait()(RETRO_MEMORY_SAVE_RAM);

                if save_data.is_null() || save_size == 0 {
                    return;
                }
                let slice = slice::from_raw_parts(save_data as *const u8, save_size);

                match File::create(&instance.save_path) {
                    Ok(mut file) => {
                        if file.write_all(slice).is_err() {
                            info!("Failed to save game.")
                        }
                    }
                    Err(_) => {
                        info!("Failed to create save file.")
                    }
                }
            }
        }
    }
}
