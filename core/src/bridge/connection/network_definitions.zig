const std = @import("std");
const Endian = std.builtin.Endian;
pub const enet = @cImport({
    @cInclude("enet/enet.h");
});

const jUUID = @import("../util/native_util.zig").jUUID;

pub const PacketType = enum(u8) {
    // Main Connection
    PACKET_AUTH = 0x00,
    PACKET_AUTH_ACK = 0x01,
    PACKET_KICK = 0x02,
    PACKET_KEEP_ALIVE = 0x03,
    // S2C
    PACKET_UPDATE_DISPLAY = 0x20,
    PACKET_UPDATE_AUDIO = 0x21,
    // C2S
    PACKET_UPDATE_CONTROLS = 0x60,
};

pub const Int8ArrayPacket = struct {
    type: PacketType,
    ref: *jUUID,
    data: std.ArrayList(u8) = std.ArrayList(u8).init(std.heap.page_allocator),

    pub fn unpack(packet: [*c]enet.struct__ENetPacket) !Int8ArrayPacket {
        const packetType: PacketType = @enumFromInt(packet.*.data[0]);
        if (packet.*.dataLength < 25) {
            std.debug.print("[RetroPacket] Int16arr packet too small ({d})", .{@intFromEnum(packetType)});
        }
        const packetSize = std.mem.readInt(usize, packet.*.data[17..25], Endian.little);
        var packetData = try std.ArrayList(u8).initCapacity(std.heap.raw_c_allocator, packetSize);
        try packetData.appendSlice(packet.*.data[25 .. packetSize + 25]);
        var uuid = jUUID.fromBytes(packet.*.data[1..17]);
        return .{ .type = packetType, .ref = &uuid, .data = packetData };
    }

    pub fn pack(self: *Int8ArrayPacket) ![*c]enet.struct__ENetPacket {
        const packetSize: usize = self.data.items.len + 25;
        var packetData: std.ArrayList(u8) = try std.ArrayList(u8).initCapacity(std.heap.raw_c_allocator, packetSize);
        try packetData.append(@intFromEnum(self.type));
        try packetData.appendSlice(&self.ref.toBytes());
        var len_bytes: [@sizeOf(usize)]u8 = undefined;
        std.mem.writeInt(usize, &len_bytes, self.data.items.len, Endian.little);
        try packetData.appendSlice(&len_bytes);
        try packetData.appendSlice(self.data.items);
        return enet.enet_packet_create(packetData.items.ptr, packetSize, 0);
    }

    pub fn deinit(self: *Int8ArrayPacket) void {
        self.data.deinit();
    }
};
