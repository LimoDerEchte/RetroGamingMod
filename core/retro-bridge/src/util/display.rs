use tracing::warn;
use crate::codec::video_decoder::{VideoDecoder, VideoDecoderAV1};

pub struct NativeDisplay {
    data: Vec<u8>,
    decoder: Box<dyn VideoDecoder>,

    changed: bool,
    width: i32,
    height: i32,
    codec: i32,
}

impl NativeDisplay {
    pub fn new(width: i32, height: i32, codec: i32, data_ptr: i32) -> Self {
        let size = (width * height * 3) as usize;
        NativeDisplay {
            data: unsafe {
                Vec::from_raw_parts(data_ptr as *mut u8, size, size)
            },
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

    // TODO: Implement transfer loop
}
