use std::fmt::Debug;
use std::sync::mpsc::Sender;
use tracing::{Event, Level};
use tracing::field::{Field, Visit};
use tracing_subscriber::Layer;
use tracing_subscriber::layer::Context;

#[derive(Debug, Clone)]
pub struct JavaLoggingEvent {
    pub message: String,
    pub log_func: String,
}

impl JavaLoggingEvent {
    pub fn new(event: &Event<'_>) -> Self {
        let mut ev = JavaLoggingEvent {
            message: String::new(),
            log_func: match *event.metadata().level() {
                Level::ERROR => "error",
                Level::WARN  => "warn",
                Level::INFO  => "info",
                Level::DEBUG => "debug",
                Level::TRACE => "trace",
            }.to_string(),
        };
        event.record(&mut ev);
        ev
    }
}

impl Visit for JavaLoggingEvent {
    fn record_debug(&mut self, field: &Field, value: &dyn Debug) {
        if field.name() == "message" {
            self.message = format!("{:?}", value);
        }
    }
}

pub struct JavaLoggingSubscriber {
    sender: Sender<JavaLoggingEvent>
}

impl JavaLoggingSubscriber {
    pub fn new(sender: Sender<JavaLoggingEvent>) -> Self {
        Self { sender }
    }
}

impl<T: tracing::Subscriber> Layer<T> for JavaLoggingSubscriber {
    fn on_event(&self, event: &Event<'_>, _ctx: Context<'_, T>) {
        self.sender.send(JavaLoggingEvent::new(event)).unwrap();
    }
}
