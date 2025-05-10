
const std = @import("std");

pub const VideoDecoderInt16 = struct {
    width: i32,
    height: i32,
    previousFrame: std.ArrayList(i16),

    pub fn init(width: i32, height: i32) VideoDecoderInt16 {
        return .{
            .width = width,
            .height = height,
            .previousFrame = std.ArrayList(i16).init(std.heap.c_allocator),
        };
    }

    fn performInverseDeltaEncoding(self: *VideoDecoderInt16, deltaFrame: *std.ArrayList(i16)) std.ArrayList(i16) {
        var decodedFrame = std.ArrayList(i16).initCapacity(std.heap.c_allocator, self.width * self.height);
        if(self.previousFrame.items.len == 0) {
            decodedFrame = deltaFrame;
        } else {
            var prevVal: i16 = 0;
            for(deltaFrame.items, self.previousFrame.items, 0..) |item, prev, i| {
                const packetDelta = prevVal - item;
                const frameDelta = prev + packetDelta;
                decodedFrame[i] = frameDelta;
                prevVal = packetDelta;
            }
        }
        self.previousFrame.clearAndFree();
        self.previousFrame = decodedFrame;
        return decodedFrame;
    }

    fn decompressWithZlib(self: *VideoDecoderInt16, compressedData: *std.ArrayList(u8)) std.ArrayList(i16) {
        var decodedFrame = std.ArrayList(i16).initCapacity(std.heap.c_allocator, self.width * self.height);
        try {
            std.compress.zlib.decompress(compressedData.items, decodedFrame.writer());
        } catch {
            std.debug.print("Failed to decompress using zlib");
        };
        return decodedFrame;
    }

    pub fn decodeFrame(self: *VideoDecoderInt16, encodedData: *std.ArrayList(u8)) std.ArrayList(i16) {
        const decompressedFrame = self.decompressWithZlib(encodedData);
        if(encodedData.items[0] == 1) {
            self.previousFrame = decompressedFrame;
            return decompressedFrame;
        }
        return self.performInverseDeltaEncoding(decompressedFrame);
    }

    pub fn decodeFrameRGB565(self: *VideoDecoderInt16, encodedData: *std.ArrayList(u8)) std.ArrayList(i16) {
        const decompressedFrame = self.decodeFrame(encodedData);
        for(decompressedFrame.items, 0..) |p, i| {
            decompressedFrame[i] = 0xFF000000 |
                                   (((p >> 11) & 0x1F) << 19) |
                                   (((p >> 5) & 0x3F) << 10) |
                                   ((p & 0x1F) << 3);
        }
        return decompressedFrame;
    }

    pub fn reset(self: *VideoDecoderInt16) void {
        self.previousFrame.clearAndFree();
    }
};
