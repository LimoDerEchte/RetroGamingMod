
const std = @import("std");

pub const GenericShared = struct {
    // Client to Host
    displayChanged: bool = true,
    display: [512*1024]u16 = std.mem.zeroes([512*1024]u16),
    audioChanged: bool = false,
    audio: [8192]i16 = std.mem.zeroes([8192]i16),
    audioSize: usize = 0,
    // Host to Client
    controls: [4]i16 = std.mem.zeroes([4]i16),
    shutdownRequested: bool = false,
    shutdownCompleted: bool = false,
};
