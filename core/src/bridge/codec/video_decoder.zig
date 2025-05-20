
const std = @import("std");

pub const VideoDecoderInt16 = struct {
    width: i32,
    height: i32,
    previousFrame: *std.ArrayList(i16),

    pub fn init(width: i32, height: i32) VideoDecoderInt16 {
        var frame = std.ArrayList(i16).init(std.heap.c_allocator);
        return .{
            .width = width,
            .height = height,
            .previousFrame = &frame,
        };
    }

    fn performInverseDeltaEncoding(self: *VideoDecoderInt16, deltaFrame: *std.ArrayList(i16)) !*std.ArrayList(i16) {
        var decodedFrame = &try std.ArrayList(i16).initCapacity(std.heap.c_allocator, @intCast(self.width * self.height));
        if(self.previousFrame.items.len == 0) {
            decodedFrame = deltaFrame;
        } else {
            var prevVal: i16 = 0;
            for(deltaFrame.items, self.previousFrame.items, 0..) |item, prev, i| {
                const packetDelta = prevVal - item;
                const frameDelta = prev + packetDelta;
                decodedFrame.items[i] = frameDelta;
                prevVal = packetDelta;
            }
        }
        self.previousFrame.clearAndFree();
        self.previousFrame = @constCast(decodedFrame);
        return @constCast(decodedFrame);
    }

    fn decompressWithZlib(self: *VideoDecoderInt16, compressedData: *std.ArrayList(u8)) !std.ArrayList(i16) {
        var decodedFrame = try std.ArrayList(i16).initCapacity(std.heap.c_allocator, @intCast(self.width * self.height));
        var u8_buffer = try std.ArrayList(u8).initCapacity(std.heap.c_allocator, @intCast(self.width * self.height * 2));
        defer u8_buffer.deinit();
        var source_stream = std.io.fixedBufferStream(compressedData.items);
        try std.compress.zlib.decompress(source_stream.reader(), u8_buffer.writer());
        for(0..decodedFrame.items.len) |i| {
            try decodedFrame.append(std.mem.readInt(i16, @ptrCast(u8_buffer.items[i*2..i*2+2]), std.builtin.Endian.little));
        }
        return decodedFrame;
    }

    pub fn decodeFrame(self: *VideoDecoderInt16, encodedData: *std.ArrayList(u8)) !*std.ArrayList(i16) {
        const decompressedFrame = &try self.decompressWithZlib(encodedData);
        if(encodedData.items[0] == 1) {
            self.previousFrame = @constCast(decompressedFrame);
            return @constCast(decompressedFrame);
        }
        return self.performInverseDeltaEncoding(@constCast(decompressedFrame));
    }

    pub fn decodeFrameRGB565(self: *VideoDecoderInt16, encodedData: *std.ArrayList(u8)) !std.ArrayList(u32) {
        var decompressedFrame = try self.decodeFrame(encodedData);
        var returnValue = try std.ArrayList(u32).initCapacity(std.heap.c_allocator, @intCast(self.width * self.height));
        for(decompressedFrame.items) |p| {
            const r: u32 = @intCast((p >> 11) & 0x1F * 255 / 31);
            const g: u32 = @intCast((p >> 5) & 0x3F * 255 / 63);
            const b: u32 = @intCast(p & 0x1F * 255 / 31);
            try returnValue.append(0xFF000000 | (r << 16) | (g << 8) | b);
        }
        decompressedFrame.clearAndFree();
        return returnValue;
    }

    pub fn reset(self: *VideoDecoderInt16) void {
        self.previousFrame.clearAndFree();
    }
};
