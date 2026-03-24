use tracing::warn;
use crate::codec::video_decoder::{VideoDecoder, VideoDecoderAV1};

#[allow(unused)]
pub struct NativeDisplay {
    data: *mut u8,
    decoder: Box<dyn VideoDecoder>,

    changed: bool,
    width: i32,
    height: i32,
    codec: i32,
}

unsafe impl Send for NativeDisplay {}
unsafe impl Sync for NativeDisplay {}

impl NativeDisplay {
    pub fn new(width: i32, height: i32, codec: i32, data_ptr: i64) -> Self {
        NativeDisplay {
            data: data_ptr as *mut u8,
            decoder: Box::new(match codec {
                0 => VideoDecoderAV1::new(width as u32, height as u32),
                _ => {
                    warn!("Invalid video codec supplied (ID {:?}). Falling back to AV1", codec);
                    VideoDecoderAV1::new(width as u32, height as u32)
                }
            }),
            changed: false,
            width,
            height,
            codec,
        }
    }

    pub fn changed(&mut self) -> bool {
        self.changed
    }

    pub fn receive(&mut self, pak: Vec<u8>) {
        (*self.decoder).submit_packet(pak);
    }

    pub fn try_transmit(&mut self) {
        if let Some(data) = self.decoder.retrieve_frame() {
            unsafe {
                self.data.copy_from(data.as_ptr(), data.len());
            }
        }
    }
}
