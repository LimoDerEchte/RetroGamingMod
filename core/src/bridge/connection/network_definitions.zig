
const std = @import("std");
const Endian = std.builtin.Endian;
const enet = @cImport(
    @cInclude("enet.h")
);

const jUUID = @import("../util/native_util.zig").jUUID;

pub const PacketType = enum(u8) {
    // Main Connection
    PACKET_AUTH                 = 0x00,
    PACKET_AUTH_ACK             = 0x01,
    PACKET_KICK                 = 0x02,
    PACKET_KEEP_ALIVE           = 0x03,
    // S2C
    PACKET_UPDATE_DISPLAY       = 0x20,
    PACKET_UPDATE_AUDIO         = 0x21,
    // C2S
    PACKET_UPDATE_CONTROLS      = 0x60,
};

pub const Int8ArrayPacket = struct {
    type: PacketType,
    ref: jUUID,
    data: std.ArrayList(u8),

    pub fn unpack(packet: [*c]enet.struct__ENetPacket) Int8ArrayPacket {
        const packetType: PacketType = @enumFromInt(packet.*.data[0]);
        if(packet.*.dataLength < 25) {
            std.debug.print("[RetroPacket] Int16arr packet too small ({s})", .{packetType});
        }
        const packetSize = std.mem.readInt(usize, packet.*.data[17..25], Endian.little);
        var packetData: std.ArrayList(u8) = try std.ArrayList(u8).initCapacity(std.heap.page_allocator, packetSize);
        packetData.appendUnalignedSliceAssumeCapacity(packet.*.data[25..packetSize+25]);
        return .{
            .type = packetType,
            .ref = jUUID.fromBytes(packet.*.data[1..17]),
            .data = packetData
        };
    }

    pub fn pack(self: *Int8ArrayPacket) [*c]enet.struct__ENetPacket {
        const packetSize: usize = self.data.items.len + 25;
        var packetData: std.ArrayList(u8) = try std.ArrayList(u8).initCapacity(std.heap.page_allocator, packetSize);
        packetData.appendAssumeCapacity(@intFromEnum(self.type));
        packetData.appendUnalignedSliceAssumeCapacity(self.ref);
        var len_bytes: [@sizeOf(usize)]u8 = undefined;
        std.mem.writeInt(&len_bytes, self.data.items.len, Endian.little);
        packetData.appendUnalignedSliceAssumeCapacity(&len_bytes);
        packetData.appendUnalignedSliceAssumeCapacity(self.data.items);
        return enet.enet_packet_create(packetData.items, packetSize, 0);
    }
};
