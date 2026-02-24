
enum PacketType {
    // Main Connection
    KeepAlive = 0x00,
    Auth      = 0x01,
    AuthAck   = 0x02,
    Kick      = 0x03,

    // Server to Client
    VideoData = 0x10,
    AudioData = 0x11,

    // Client to Server
    Controls  = 0x12,
}
