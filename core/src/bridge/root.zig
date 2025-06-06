// Codec
const videoDecoder = @import("codec/video_decoder.zig");
const videoEncoder = @import("codec/video_encoder.zig");
// Network
const client = @import("connection/client.zig");
const networkDef = @import("connection/network_definitions.zig");
const server = @import("connection/server.zig");
// Console
const genericConsole = @import("platform/generic_console.zig");
const consoleRegistry = @import("platform/generic_console_registry.zig");
// Util
const nativeDisplay = @import("util/native_display.zig");
const nativeUtil = @import("util/native_util.zig");

const std = @import("std");
const jni = @import("jni");

comptime {
    jni.exportJNI("com.limo.emumod.client.bridge.NativeClient", client);
    jni.exportJNI("com.limo.emumod.bridge.NativeDisplay", nativeDisplay);
    jni.exportJNI("com.limo.emumod.bridge.NativeGenericConsole", genericConsole);
    jni.exportJNI("com.limo.emumod.bridge.NativeServer", server);
    jni.exportJNI("com.limo.emumod.bridge.NativeUtil", nativeUtil);
}

// Tests
test {
    std.testing.refAllDecls(@This());
}

test "Test Packing and Unpacking" {
    std.debug.print(" === Test Packing and Unpacking ===\n", .{});
    const data = "This is a test :D";
    var arr: std.ArrayList(u8) = std.ArrayList(u8).init(std.heap.page_allocator);
    try arr.appendSlice(data);
    var packet: networkDef.Int8ArrayPacket = .{
        .type = networkDef.PacketType.PACKET_KICK,
        .ref = @constCast(&nativeUtil.jUUID{
            .leastSignificantBits = 12345,
            .mostSignificantBits = 78910,
        }),
        .data = arr,
    };
    const pack = try packet.pack();
    const unpacked = try networkDef.Int8ArrayPacket.unpack(pack);
    try std.testing.expect(unpacked.type == networkDef.PacketType.PACKET_KICK);
    std.debug.print("Decoded: {s}\n", .{unpacked.data.items});
    try std.testing.expectEqualStrings(data, unpacked.data.items);
}
