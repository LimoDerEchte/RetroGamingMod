use std::error::Error;
use retro_shared::shared::shared_memory::SharedMemory;

pub trait AudioDecoder: Send {
    fn new() -> Self where Self: Sized;
    fn decode_frame(&mut self, data: Vec<u8>) -> Result<Vec<i16>, Box<dyn Error>>;
}

pub struct AudioDecoderOpus {
    decoder: opus::Decoder,
}

impl AudioDecoder for AudioDecoderOpus {
    fn new() -> Self {
        Self {
            decoder: opus::Decoder::new(48000, opus::Channels::Stereo)
                .expect("Failed to initialize Opus decoder!")
        }
    }

    fn decode_frame(&mut self, data: Vec<u8>) -> Result<Vec<i16>, Box<dyn Error>> {
        let mut output_buffer = vec![0i16; SharedMemory::AUDIO_FRAME_SIZE];
        let size = self.decoder.decode(data.as_slice(), output_buffer.as_mut_slice(), false)?;

        output_buffer.truncate(size);
        Ok(output_buffer)
    }
}
