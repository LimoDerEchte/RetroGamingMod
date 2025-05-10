
const std = @import("std");
const Endian = std.builtin.Endian;

const idCharset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

pub const jUUID = struct {
    mostSignificantBits: i64,
    leastSignificantBits: i64,

    pub fn combine(self: *jUUID) i64 {
        return self.mostSignificantBits ^ self.leastSignificantBits << 1;
    }

    pub fn toBytes(self: *jUUID) [16]u8 {
        var bytes: [16]u8 = std.mem.zeroes([16]u8);
        std.mem.writeInt(i64, bytes[0..8], self.leastSignificantBits, Endian.little);
        std.mem.writeInt(i64, bytes[8..16], self.mostSignificantBits, Endian.little);
        return bytes;
    }

    pub fn fromBytes(bytes: *[16]u8) jUUID {
        return .{
            .leastSignificantBits = std.mem.readInt(i64, bytes[0..8], Endian.little),
            .mostSignificantBits = std.mem.readInt(i64, bytes[8..16], Endian.little)
        };
    }
};

pub fn GenerateID(id: *[32]u8) !void {
    var prng = std.Random.DefaultPrng.init(blk: {
        var seed: u64 = undefined;
        try std.posix.getrandom(std.mem.asBytes(&seed));
        break :blk seed;
    });
    const rand = prng.random();
    for(0..id.len) |i| {
        id[i] = idCharset[rand.intRangeLessThan(u8, 0, idCharset.len)];
    }
}
