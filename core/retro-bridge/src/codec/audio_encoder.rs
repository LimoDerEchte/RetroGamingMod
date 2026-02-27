use std::error::Error;

pub trait AudioEncoder {
    fn new() -> Self where Self: Sized;
    fn encode_frame(&mut self, data: Vec<u16>) -> Result<Vec<u8>, Box<dyn Error>>;
}
