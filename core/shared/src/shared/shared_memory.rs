
#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct SharedMemory {
    // Core to Bridge
    pub display_changed: bool,
    pub display_data: [u32; 4 * 720 * 480], // Reserve 480p ARGB data

    pub audio_changed: bool,
    pub audio_data: [i16; SharedMemory::AUDIO_FRAME_SIZE],

    // Bridge to Core
    pub controls: [u16; 4], // Reserve 16 bit gamepad for 4 users

    pub shutdown_requested: bool,
}

impl SharedMemory {
    pub const AUDIO_FRAME_SIZE: usize = 1920; // 40 ms at 48 kHz
}
