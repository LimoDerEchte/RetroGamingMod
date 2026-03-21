use crate::util::logging::{JavaLoggingEvent, JavaLoggingSubscriber};
use jni::sys::{jint, JNI_VERSION_1_8};
use jni::{jni_sig, jni_str, sys, JValue, JavaVM};
use std::error::Error;
use std::sync::mpsc::channel;
use jni::strings::JNIString;
use tracing_subscriber::{layer::SubscriberExt, Registry};

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(ptr: *mut sys::JavaVM, _: *mut std::os::raw::c_void) -> jint {
    let vm: JavaVM = unsafe { JavaVM::from_raw(ptr) };

    std::thread::spawn(move || {
        vm.attach_current_thread(|env| -> Result<(), Box<dyn Error>> {
            let (tx, rx) = channel::<JavaLoggingEvent>();

            let subscriber = Registry::default()
                .with(JavaLoggingSubscriber::new(tx));

            tracing::subscriber::set_global_default(subscriber)
                .expect("Failed to set global tracing subscriber to JavaLoggingSubscriber");

            let class = env.find_class(jni_str!("com/limo/emumod/EmuMod"))?;
            let logger = env.get_static_field(
                class,
                jni_str!("LOGGER"),
                jni_sig!(org.apache.logging.log4j.Logger),
            )?.l()?;

            loop {
                let event = rx.recv()?;
                let msg = env.new_string(event.message)?;

                env.call_method(
                    &logger,
                    JNIString::from(event.log_func),
                    jni_sig!((message: java.lang.String) -> void),
                    &[ JValue::Object(&msg) ]
                )?;
            }
        }).unwrap();
    });

    JNI_VERSION_1_8
}
