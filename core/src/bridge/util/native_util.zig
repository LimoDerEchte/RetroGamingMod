
const std = @import("std");
const random = std.Random.DefaultPrng;

const idCharset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

const jUUID = struct {
    mostSignificantBits: i64,
    leastSignificantBits: i64,

    fn combine(self: *jUUID) i64 {
        return self.mostSignificantBits ^ self.leastSignificantBits << 1;
    }
};

pub fn GenerateID(id: *[32]u8) !void {
    var prng = random.init(blk: {
        var seed: u64 = undefined;
        try std.posix.getrandom(std.mem.asBytes(&seed));
        break :blk seed;
    });
    const rand = prng.random();
    for(0..id.len) |i| {
        id[i] = idCharset[rand.intRangeLessThan(u8, 0, idCharset.len)];
    }
}

test "Test ID Generation" {
    var id = std.mem.zeroes([32]u8);
    try GenerateID(&id);
    std.debug.print("Generated ID: {s}\n", .{ id });
}

test "Test jUUID Combination" {
    var id: jUUID = .{
        .leastSignificantBits = 100000,
        .mostSignificantBits = 222222,
    };
    std.debug.print("Combined Number: {d}\n", .{ id.combine() });
}
