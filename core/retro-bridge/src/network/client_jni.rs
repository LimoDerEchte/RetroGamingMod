use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jint, jshort};
use jni::Env;
use crate::network::client::RetroClient;

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_init<'caller>(
    env: &mut Env<'caller>,
    _: JClass<'caller>,
    j_token: JByteArray<'caller>
) -> jboolean {

    let token: Vec<u8> = match env.convert_byte_array(j_token) {
        Ok(token) => token,
        Err(_) => return false,
    };

    if RetroClient::init(token).is_err() {
        return false
    }

    std::thread::spawn(|| {
        RetroClient::main_loop();
    });
    true
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_deinit<'caller>(
    _: &mut Env<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    RetroClient::deinit().is_ok()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_isConnected<'caller>(
    _: &mut Env<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    RetroClient::with_instance(|instance| {
        Ok(instance.is_connected())
    }).unwrap()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_registerId<'caller>(
    _: &mut Env<'caller>,
    _: JClass<'caller>,
    id: jint,
    width: jint,
    height: jint,
    codec: jint,
    display_data_ptr: jint
) {
    RetroClient::with_instance(|instance| {
        instance.register_id(id, width, height, codec, display_data_ptr);
        Ok(())
    }).unwrap();
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_unregisterId<'caller>(
    _: &mut Env<'caller>,
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
    _: &mut Env<'caller>,
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
