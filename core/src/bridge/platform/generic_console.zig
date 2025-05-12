
const std = @import("std");
const VideoEncoderInt16 = @import("../codec/video_encoder.zig").VideoEncoderInt16;
const consoleRegistry = @import("generic_console_registry.zig").registryInstance;
const native = @import("../util/native_util.zig");

pub const GenericConsole = struct {
    videoEncoder: VideoEncoderInt16 = undefined,
    uuid: native.jUUID,

    id: [32]u8,
    width: i32,
    height: i32,
    sampleRate: i32,

    fn init(width: i32, height: i32, sampleRate: i32, uuid: native.jUUID) GenericConsole {
        var id: [32]u8 = std.mem.zeroes([32]u8);
        native.GenerateID(&id);
        return .{
            .uuid = uuid,
            .id = id,
            .width = width,
            .height = height,
            .sampleRate = sampleRate
        };
    }
};
