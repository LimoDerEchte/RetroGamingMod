use std::error::Error;

pub trait AudioEncoder: Send {
    fn new() -> Self where Self: Sized;
    fn encode_frame(&mut self, data: Vec<i16>) -> Result<Vec<u8>, Box<dyn Error>>;
}

pub struct AudioEncoderOpus {
    encoder: opus::Encoder,
}

impl AudioEncoder for AudioEncoderOpus {
    fn new() -> Self {
        Self {
            encoder: opus::Encoder::new(48000, opus::Channels::Stereo, opus::Application::LowDelay)
                .expect("Failed to initialize audio encoder"),
        }
    }

    fn encode_frame(&mut self, data: Vec<i16>) -> Result<Vec<u8>, Box<dyn Error>> {
        let mut output_buffer = vec![0u8; 1024];
        let size = self.encoder.encode(data.as_slice(), output_buffer.as_mut_slice())?;

        output_buffer.truncate(size);
        Ok(output_buffer)
    }
}
