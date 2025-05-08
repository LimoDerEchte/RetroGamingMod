
const GenericShared = struct {
    // Client to Host
    displayChanged: bool = true,
    display: u16[512*1024] = {},
    audioChanged: bool = false,
    audio: i16[8192] = {},
    audioSize: usize = 0,
    // Host to Client
    controls: i16[4] = {},
    shutdownRequested: bool = false,
    shutdownCompleted: bool = false,
};
