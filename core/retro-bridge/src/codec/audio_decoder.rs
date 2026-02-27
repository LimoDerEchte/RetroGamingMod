use std::error::Error;
use audiopus::coder::Decoder;
use audiopus::{Channels, SampleRate};
use retro_shared::shared::shared_memory::SharedMemory;

pub trait AudioDecoder {
    fn new() -> Self where Self: Sized;
    fn decode_frame(&mut self, data: Vec<u8>) -> Result<Vec<i16>, Box<dyn Error>>;
}

pub struct AudioDecoderOpus {
    decoder: Decoder,
}

impl AudioDecoder for AudioDecoderOpus {
    fn new() -> Self {
        Self {
            decoder: Decoder::new(SampleRate::Hz48000, Channels::Stereo)
                .expect("Failed to initialize audio decoder"),
        }
    }

    fn decode_frame(&mut self, data: Vec<u8>) -> Result<Vec<i16>, Box<dyn Error>> {
        let mut output_buffer = vec![0i16; SharedMemory::AUDIO_FRAME_SIZE];
        let size = self.decoder.decode(Some(data.as_slice()), output_buffer.as_mut_slice(), false)?;

        output_buffer.truncate(size);
        Ok(output_buffer)
    }
}
