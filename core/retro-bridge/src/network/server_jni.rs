use crate::network::server::RetroServer;
use jni::objects::{JByteArray, JClass, JObjectArray, JString};
use jni::sys::jboolean;
use jni::Env;

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_init<'caller>(
    env: &mut Env<'caller>,
    _: JClass<'caller>,
    max_users: i32,
    j_bind: JString<'caller>,
    j_addresses: JObjectArray<'caller>,
) -> jboolean {

    let bind: String = match j_bind.try_to_string(env) {
        Ok(val) => val,
        Err(_) => return false
    };

    let addr_len = match j_addresses.len(env) {
        Ok(val) => val,
        Err(_) => return false
    };

    let mut addresses: Vec<String> = Vec::with_capacity(addr_len);
    for i in 0..addr_len {
        let address: JString = match j_addresses.get_element(env, i) {
            Ok(val) => unsafe { JString::from_raw(env, val.as_raw()) },
            Err(_) => return false
        };
        addresses.push(match address.try_to_string(env) {
            Ok(val) => val,
            Err(_) => return false
        });
    }

    if RetroServer::init(max_users, bind.as_str(), addresses).is_err() {
        return false
    }

    std::thread::spawn(|| {
        RetroServer::main_loop();
    });
    true
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_deinit<'caller>(
    _: &mut Env<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    RetroServer::deinit().is_ok()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_generateToken<'caller>(
    env: &mut Env<'caller>,
    _: JClass<'caller>,
) -> JByteArray<'caller> {

    RetroServer::with_instance(|instance| {
        let tok = instance.gen_token()?;
        Ok(env.byte_array_from_slice(tok.as_slice())?)
    }).unwrap_or(env.new_byte_array(0).unwrap())
}
