use jni::objects::{JByteArray, JClass};
use jni::sys::jboolean;
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
    RetroClient::init(token).is_ok()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_deinit<'caller>(
    env: &mut Env<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    RetroClient::deinit().is_ok()
}
