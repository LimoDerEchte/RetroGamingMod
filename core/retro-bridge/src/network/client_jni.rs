use crate::network::client::RetroClient;
use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jint, jlong, jshort};
use jni::EnvUnowned;
use tracing::{info};

#[unsafe(export_name = "Java_com_limo_emumod_client_bridge_NativeClient_init")]
pub extern "system" fn native_client_init<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    j_token: JByteArray<'caller>
) -> jboolean {

    unowned.with_env(|env| -> Result<_, jni::errors::Error> {
        let token = env.convert_byte_array(j_token)?;

        RetroClient::init(token);
        std::thread::spawn(|| { RetroClient::main_loop(); });
        std::thread::spawn(|| { RetroClient::video_receiving_loop(); });

        info!("RetroClient initialized and started");
        Ok(())
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>();
    true
}

#[unsafe(export_name = "Java_com_limo_emumod_client_bridge_NativeClient_deinit")]
pub extern "system" fn native_client_deinit<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    unowned.with_env(|_| -> Result<_, jni::errors::Error> {
        Ok(RetroClient::deinit().is_ok())
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(export_name = "Java_com_limo_emumod_client_bridge_NativeClient_isConnected")]
pub extern "system" fn native_client_connected<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    unowned.with_env(|_| -> Result<_, jni::errors::Error> {
        Ok(RetroClient::with_instance(|instance| {
            Ok(instance.is_connected())
        }).unwrap())
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(export_name = "Java_com_limo_emumod_client_bridge_NativeClient_registerId")]
pub extern "system" fn native_client_register_id<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
    width: jint,
    height: jint,
    video_codec: jint,
    display_data_ptr: jlong,
    audio_codec: jint,
) {

    unowned.with_env(|_| -> Result<_, jni::errors::Error> {
        RetroClient::with_instance(|instance| {
            instance.register_id(id, width, height, video_codec, display_data_ptr, audio_codec);
            Ok(())
        }).unwrap();
        Ok(())
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(export_name = "Java_com_limo_emumod_client_bridge_NativeClient_unregisterId")]
pub extern "system" fn native_client_unregister_id<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
) {

    unowned.with_env(|_| -> Result<_, jni::errors::Error> {
        RetroClient::with_instance(|instance| {
            instance.unregister_id(id);
            Ok(())
        }).unwrap();
        Ok(())
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(export_name = "Java_com_limo_emumod_client_bridge_NativeClient_sendControls")]
pub extern "system" fn native_client_send_controls<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
    port: jshort,
    data: jshort,
) {

    unowned.with_env(|_| -> Result<_, jni::errors::Error> {
        RetroClient::with_instance(|instance| {
            instance.send_input_data(id, port, data);
            Ok(())
        }).unwrap();
        Ok(())
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(export_name = "Java_com_limo_emumod_client_bridge_NativeClient_screenChanged")]
pub extern "system" fn native_client_display_changed<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    id: jint,
) -> jboolean {

    unowned.with_env(|_| -> Result<_, jni::errors::Error> {
        Ok(RetroClient::with_instance(|instance| {
            Ok(instance.with_display(id, |display| {
                Ok(display.changed())
            }).unwrap_or(false))
        }).unwrap_or(false))
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
