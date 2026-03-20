use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jint, jlong, jshort};
use jni::{Env, EnvUnowned};
use crate::network::client::RetroClient;

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_init<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    j_token: JByteArray<'caller>
) -> jboolean {

    let token = unowned.with_env(|env| {
        env.convert_byte_array(j_token)
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>();

    if RetroClient::init(token).is_err() {
        return false
    }

    std::thread::spawn(|| { RetroClient::main_loop(); });
    std::thread::spawn(|| { RetroClient::video_receiving_loop(); });
    true
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_deinit<'caller>(
    _: EnvUnowned<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    RetroClient::deinit().is_ok()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_isConnected<'caller>(
    _: EnvUnowned<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    RetroClient::with_instance(|instance| {
        Ok(instance.is_connected())
    }).unwrap()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_registerId<'caller>(
    _: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
    width: jint,
    height: jint,
    video_codec: jint,
    display_data_ptr: jlong,
    audio_codec: jint,
) {
    RetroClient::with_instance(|instance| {
        instance.register_id(id, width, height, video_codec, display_data_ptr, audio_codec);
        Ok(())
    }).unwrap();
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_unregisterId<'caller>(
    _: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
) {
    RetroClient::with_instance(|instance| {
        instance.unregister_id(id);
        Ok(())
    }).unwrap();
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_sendControls<'caller>(
    _: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
    port: jshort,
    data: jshort,
) {
    RetroClient::with_instance(|instance| {
        instance.send_input_data(id, port, data);
        Ok(())
    }).unwrap();
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_screenChanged<'caller>(
    _: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
) -> jboolean {
    RetroClient::with_instance(|instance| {
        Ok(instance.with_display(id, |display| {
            Ok(display.changed())
        }).unwrap_or(false))
    }).unwrap_or(false)
}
