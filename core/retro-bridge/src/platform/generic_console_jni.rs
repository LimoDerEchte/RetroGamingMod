use jni::{EnvUnowned};
use jni::objects::{JClass, JString};
use jni::sys::{jint};
use tracing::{info, warn};
use crate::platform::generic_console::ConsoleRegistry;

#[unsafe(export_name = "Java_com_limo_emumod_bridge_NativeGenericConsole_register")]
pub extern "system" fn generic_console_register<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    width: jint,
    height: jint,
    video_codec: jint,
    audio_codec: jint,
) -> jint {

    let id = unowned.with_env(|_| -> Result<_, jni::errors::Error> {
        Ok(ConsoleRegistry::register_new(width, height, video_codec, audio_codec))
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>();
    info!("Console registered: {:?}", id);
    id
}

#[unsafe(export_name = "Java_com_limo_emumod_bridge_NativeGenericConsole_unregister")]
pub extern "system" fn generic_console_unregister<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
) {

    unowned.with_env(|_| -> Result<_, jni::errors::Error> {
        ConsoleRegistry::unregister(id);
        Ok(())
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}

#[unsafe(export_name = "Java_com_limo_emumod_bridge_NativeGenericConsole_start")]
pub extern "system" fn generic_console_start<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
    j_retro_core: JString<'caller>,
    j_core: JString<'caller>,
    j_rom: JString<'caller>,
    j_save: JString<'caller>,
) {

    unowned.with_env(|env| -> Result<_, jni::errors::Error> {
        let retro_core = j_retro_core.try_to_string(env)?;
        let core = j_core.try_to_string(env)?;
        let rom = j_rom.try_to_string(env)?;
        let save = j_save.try_to_string(env)?;

        if ConsoleRegistry::with_console(id, |console| {
            console.load(retro_core, core, rom, save)
        }).is_err() {
            warn!("Failed to start console!")
        };
        Ok(())
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}
