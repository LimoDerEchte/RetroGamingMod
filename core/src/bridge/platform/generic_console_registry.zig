const std = @import("std");
const GenericConsole = @import("generic_console.zig").GenericConsole;
const jUUID = @import("../util/native_util.zig").jUUID;

pub var registryInstance: GenericConsoleRegistry = .{
    .mutex = .{},
    .consoles = std.ArrayList(*GenericConsole).init(std.heap.page_allocator),
};

pub const GenericConsoleRegistry = struct {
    mutex: std.Thread.Mutex,
    consoles: std.ArrayList(*GenericConsole),

    pub fn register(self: *GenericConsoleRegistry, console: *GenericConsole) !void {
        self.mutex.lock();
        try self.consoles.append(console);
        self.mutex.unlock();
    }

    pub fn unregister(self: *GenericConsoleRegistry, console: *GenericConsole) void {
        self.mutex.lock();
        var rmIndex: ?usize = null;
        for (self.consoles.items, 0..) |ptr, i| {
            if (ptr == console) {
                rmIndex = i;
                break;
            }
        }
        if (rmIndex != null) {
            _ = self.consoles.swapRemove(rmIndex.?);
        }
        self.mutex.unlock();
    }

    pub fn findConsoleUnsafe(self: *GenericConsoleRegistry, uuid: *jUUID) ?*GenericConsole {
        for (self.consoles.items) |console| {
            if (console.uuid.eq(uuid)) {
                return console;
            }
        }
        return null;
    }
};
