
pub trait VideoDecoder {
    fn submit_packet(&self, data: &[u8]);
    fn retrieve_frame(&self) -> Option<Vec<u32>>;
}
