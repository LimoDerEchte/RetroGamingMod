use std::error::Error;

pub trait AudioDecoder {
    fn new() -> Self where Self: Sized;
    fn decode_frame(&mut self, data: Vec<u8>) -> Result<Vec<u16>, Box<dyn Error>>;
}
