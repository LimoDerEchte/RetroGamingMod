const std = @import("std");

const RawFrameInterval: i32 = 30;

pub const VideoEncoderInt16 = struct {
    width: i32,
    height: i32,
    frameCount: i32 = 0,
    previousFrame: ?std.ArrayList(i16),

    pub fn init(width: i32, height: i32) !VideoEncoderInt16 {
        return .{
            .width = width,
            .height = height,
            .previousFrame = null,
        };
    }

    fn performDeltaEncoding(self: *VideoEncoderInt16, currentFrame: std.ArrayList(i16)) !std.ArrayList(i16) {
        var deltaEncoded = try std.ArrayList(i16).initCapacity(std.heap.page_allocator, @intCast(self.width * self.height));
        if (self.previousFrame == null) {
            deltaEncoded = currentFrame;
        } else {
            var prevVal: i16 = 0;
            for (currentFrame.items, self.previousFrame.?.items, 0..) |item, prev, i| {
                const frameDelta = item - prev;
                const packetDelta = prevVal - frameDelta;
                deltaEncoded.items[i] = packetDelta;
                prevVal = frameDelta;
            }
            self.previousFrame.?.deinit();
        }
        self.previousFrame = currentFrame;
        return deltaEncoded;
    }

    fn compressWithZlib(self: *VideoEncoderInt16, data: std.ArrayList(i16), isRawFrame: bool) !std.ArrayList(u8) {
        var encodedFrame = try std.ArrayList(u8).initCapacity(std.heap.page_allocator, @intCast(self.width * self.height * 2 + 1));
        var writer = encodedFrame.writer();
        try writer.writeByte(@intFromBool(isRawFrame));
        var stream = std.io.fixedBufferStream(std.mem.bytesAsSlice(u8, data.items));
        std.compress.zlib.compress(stream.reader(), writer, .{ .level = @enumFromInt(9) }) catch {
            std.debug.print("[RetroServer] Failed to compress using zlib", .{});
        };
        return encodedFrame;
    }

    pub fn encodeFrame(self: *VideoEncoderInt16, frame: std.ArrayList(i16)) !std.ArrayList(u8) {
        defer frame.deinit();
        self.frameCount += 1;
        if (@mod(self.frameCount, RawFrameInterval) == 0) {
            self.frameCount = 0;
            self.previousFrame = frame;
            return self.compressWithZlib(frame, true);
        }
        return self.compressWithZlib(try self.performDeltaEncoding(frame), false);
    }

    pub fn reset(self: *VideoEncoderInt16) void {
        self.previousFrame.deinit();
        self.frameCount = 0;
    }
};
