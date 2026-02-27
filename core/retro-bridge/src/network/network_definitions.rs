
pub enum PacketType {
    Kick      = 0x00,
    VideoData = 0x01,
    AudioData = 0x02,
    Controls  = 0x03,
    Invalid = 0xFF,
}

impl From<u8> for PacketType {
    fn from(v: u8) -> Self {
        match v {
            0x00 => PacketType::Kick,
            0x01 => PacketType::VideoData,
            0x02 => PacketType::AudioData,
            0x03 => PacketType::Controls,
            _ => PacketType::Invalid,
        }
    }
}
