const std = @import("std");
const shm = @import("shared_memory");
const shared = @import("shared");
const GenericConsole = @import("platform/generic_console.zig");

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();
    const allocator = gpa.allocator();

    const args = try std.process.argsAlloc(allocator);
    defer std.process.argsFree(allocator, args);

    if (args.len < 6) {
        std.debug.print("This should NEVER be called by a user (too few arguments)\n", .{});
        std.debug.print("Usage: retro-core <platform> <id> <core> <rom> <save>\n", .{});
        return;
    }

    const platform = args[1];
    const id = args[2];
    const core = args[3];
    const rom = args[4];
    const save = args[5];

    const sharedData = shm.SharedMemory(shared.GenericShared).open(id, allocator) catch |err| {
        std.debug.print("[RetroGamingCore] Failed to open shared memory: {any}\n", .{err});
        return;
    };

    if (std.mem.eql(u8, platform, "gn")) {
        const result = GenericConsole.load(allocator, sharedData.data, core, rom, save) catch |err| {
            std.debug.print("[RetroGamingCore] Error in GenericConsole.load: {any}\n", .{err});
            return;
        };
        std.process.exit(@intCast(result));
    }

    std.debug.print("This should NEVER be called by a user (unknown platform {s})\n", .{platform});
}
