use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jint, jlong};
use jni::Env;

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_limo_emumod_client_bridge_NativeClient_connect<'caller>(env: &mut Env<'caller>, _: JClass<'caller>, j_ip: JString<'caller>, port: jint, j_token: JByteArray<'caller>) -> jlong {
    let ip: String = match j_ip.try_to_string(env) {
        Ok(ip) => ip,
        Err(_) => return -1
    };

    let token: Vec<u8> = match env.convert_byte_array(j_token) {
        Ok(token) => token,
        Err(_) => return -1,
    };

    -1
}
