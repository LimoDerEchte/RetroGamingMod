
const std = @import("std");

const RawFrameInterval = 30;

pub const VideoEncoderInt16 = struct {
    width: i32,
    height: i32,
    frameCount: i32 = 0,
    previousFrame: std.ArrayList(i16),

    pub fn init(width: i32, height: i32) !VideoEncoderInt16 {
        return .{
            .width = width,
            .height = height,
            .previousFrame = try std.ArrayList(i16).init(std.heap.c_allocator),
        };
    }

    fn performDeltaEncoding(self: *VideoEncoderInt16, currentFrame: std.ArrayList(i16)) !std.ArrayList(i16) {
        var deltaEncoded = try std.ArrayList(i16).initCapacity(std.heap.c_allocator, self.width * self.height);
        if(self.previousFrame.items.len == 0) {
            deltaEncoded = currentFrame;
        } else {
            var prevVal: i16 = 0;
            for(currentFrame.items, self.previousFrame.items, 0..) |item, prev, i| {
                const frameDelta = item - prev;
                const packetDelta = prevVal - frameDelta;
                deltaEncoded[i] = packetDelta;
                prevVal = frameDelta;
            }
        }
        self.previousFrame.clearAndFree();
        self.previousFrame = currentFrame;
        return deltaEncoded;
    }

    fn compressWithZlib(self: *VideoEncoderInt16, data: std.ArrayList(i16), isRawFrame: bool) !std.ArrayList(u8) {
        var encodedFrame = try std.ArrayList(u8).initCapacity(std.heap.c_allocator, self.width * self.height * 2 + 1);
        try {
            var writer = encodedFrame.writer();
            writer.writeByte(@intFromBool(isRawFrame));
            std.compress.zlib.compress(data.items, writer, .{
                .level = std.compress.zlib.Level.best
            });
        } catch {
            std.debug.print("Failed to compress using zlib");
        };
        return encodedFrame;
    }

    pub fn encodeFrame(self: *VideoEncoderInt16, frame: std.ArrayList(i16)) !std.ArrayList(u8) {
        self.frameCount += 1;
        if(self.frameCount % RawFrameInterval == 0) {
            self.previousFrame = frame;
            return self.compressWithZlib(frame, true);
        }
        return self.compressWithZlib(self.performDeltaEncoding(frame), false);
    }

    pub fn reset(self: *VideoEncoderInt16) void {
        self.previousFrame.clearAndFree();
        self.frameCount = 0;
    }
};
