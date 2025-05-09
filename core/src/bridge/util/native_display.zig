
const std = @import("std");
const decoder = @import("../codec/video_decoder.zig");

const NativeDisplay = struct {
    decoder: ?decoder.VideoDecoderInt16 = null,
    mutex: std.Thread.Mutex = {},
    changed: bool = false,
    buf: std.ArrayList(u32),
    width: i32,
    height: i32,

    pub fn init(width: i32, height: i32) NativeDisplay {
        return .{
            .buf = std.ArrayList(u32).init(std.heap.page_allocator),
            .width = width,
            .height = height,
        };
    }

    pub fn receive(self: *NativeDisplay, data: std.ArrayList(u8)) void {
        self.mutex.lock();
        if(self.decoder == null) {
            self.decoder = decoder.VideoDecoderInt16.init(self.width, self.height);
        }
        self.decoder.?.decodeFrameRGB565(data);
        self.changed = true;
    }
};
