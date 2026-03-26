use crate::util::logging::{JavaLoggingEvent, JavaLoggingSubscriber};
use jni::sys::{jint, JNI_VERSION_1_8};
use jni::{jni_sig, jni_str, sys, AttachConfig, JValue, JavaVM, DEFAULT_LOCAL_FRAME_CAPACITY};
use std::error::Error;
use std::sync::mpsc::channel;
use jni::strings::JNIString;
use tracing_subscriber::{layer::SubscriberExt, Registry};

/// # Safety
/// Clippy wants me to write something so here you go
#[unsafe(export_name = "JNI_OnLoad")]
pub unsafe extern "system" fn jni_on_load(ptr: *mut sys::JavaVM, _: *mut std::os::raw::c_void) -> jint {
    let vm: JavaVM = unsafe { JavaVM::from_raw(ptr) };

    std::thread::spawn(move || {
        vm.attach_current_thread_with_config(
            || AttachConfig::default()
                .thread_name(jni_str!("Tracing Proxy")),
            Some(DEFAULT_LOCAL_FRAME_CAPACITY),
            |env| -> Result<(), Box<dyn Error>> {
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
            }
        ).expect("Failed to attach current thread to JVM!");
    });

    JNI_VERSION_1_8
}
