use crate::network::server::RetroServer;
use jni::objects::{JByteArray, JClass, JObjectArray, JString};
use jni::sys::jboolean;
use jni::EnvUnowned;
use tracing::warn;

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_bridge_NativeServer_init<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
    max_users: i32,
    j_bind: JString<'caller>,
    j_addresses: JObjectArray<'caller>,
) -> jboolean {

    unowned.with_env(|env| -> Result<_, jni::errors::Error> {
        let bind: String = j_bind.try_to_string(env)?;
        let addr_len = j_addresses.len(env)?;

        let mut addresses: Vec<String> = Vec::with_capacity(addr_len);
        for i in 0..addr_len {
            let element = j_addresses.get_element(env, i)?;
            let address = unsafe { JString::from_raw(env, element.as_raw()) };
            addresses.push(address.try_to_string(env)?);
        }

        if let Err(err) = RetroServer::init(max_users, bind.as_str(), addresses) {
            warn!("Failed to initialize RetroServer: {:?}", err);
            return Ok(false)
        }

        std::thread::spawn(|| { RetroServer::main_loop(); });
        std::thread::spawn(|| { RetroServer::video_packing_loop(); });

        Ok(true)
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_bridge_NativeServer_deinit<'caller>(
    _: EnvUnowned<'caller>,
    _: JClass<'caller>,
) -> jboolean {

    RetroServer::deinit().is_ok()
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_bridge_NativeServer_generateToken<'caller>(
    mut unowned: EnvUnowned<'caller>,
    _: JClass<'caller>,
) -> JByteArray<'caller> {

    unowned.with_env(|env| -> Result<_, jni::errors::Error> {
        Ok(RetroServer::with_instance(|instance| {
            let tok = instance.gen_token()?;
            Ok(env.byte_array_from_slice(tok.as_slice())?)
        }).unwrap_or(env.new_byte_array(0)?))
    }).resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
