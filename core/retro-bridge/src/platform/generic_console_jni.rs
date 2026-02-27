use jni::Env;
use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jint, jlong};
use tracing::warn;
use crate::platform::generic_console::ConsoleRegistry;

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_bridge_NativeGenericConsole_register<'caller>(
    _: &mut Env<'caller>,
    _: JClass<'caller>,
    width: jint,
    height: jint,
    video_codec: jint,
    audio_codec: jint,
) -> jint {
    ConsoleRegistry::register_new(width, height, video_codec, audio_codec)
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_bridge_NativeGenericConsole_unregister<'caller>(
    _: &mut Env<'caller>,
    _: JClass<'caller>,
    id: jint,
) {
    ConsoleRegistry::unregister(id)
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_bridge_NativeGenericConsole_start<'caller>(
    env: &mut Env<'caller>,
    _: JClass<'caller>,
    id: jint,
    j_retro_core: JString<'caller>,
    j_core: JString<'caller>,
    j_rom: JString<'caller>,
    j_save: JString<'caller>,
) {

    let retro_core: String = match j_retro_core.try_to_string(env) {
        Ok(val) => val,
        Err(_) => return
    };

    let core: String = match j_core.try_to_string(env) {
        Ok(val) => val,
        Err(_) => return
    };

    let rom: String = match j_rom.try_to_string(env) {
        Ok(val) => val,
        Err(_) => return
    };

    let save: String = match j_save.try_to_string(env) {
        Ok(val) => val,
        Err(_) => return
    };

    if ConsoleRegistry::with_console_mut(id, |console| {
        console.load(retro_core, core, rom, save)
    }).is_err() {
        warn!("Failed to start console!")
    }
}
