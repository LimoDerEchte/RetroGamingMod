
pub struct SharedMemory {
    // Core to Bridge
    pub display_changed: bool,
    pub display_data: [u32; 4 * 720 * 480], // Reserve 480p ARGB data

    pub audio_changed: bool,
    pub audio_data: [u32; 8192], // Reserve 8 Kb audio chunks

    pub shutdown_completed: bool,

    // Bridge to Core
    pub controls: [u16; 4], // Reserve 16 bit gamepad for 4 users

    pub shutdown_requested: bool,
}
