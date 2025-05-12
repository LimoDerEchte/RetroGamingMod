
const std = @import("std");
const GenericConsole = @import("generic_console.zig").GenericConsole;
const jUUID = @import("../util/native_util.zig").jUUID;

pub const registryInstance: GenericConsoleRegistry = .{
    .mutex = .{},
    .consoles = std.ArrayList(*GenericConsole).init(std.heap.c_allocator)
};

pub const GenericConsoleRegistry = struct {
    mutex: std.Thread.Mutex,
    consoles: std.ArrayList(*GenericConsole),

    pub fn register(self: *GenericConsoleRegistry, console: *GenericConsole) void {
        self.mutex.lock();
        self.consoles.append(console);
        self.mutex.unlock();
    }

    pub fn unregister(self: *GenericConsoleRegistry, console: *GenericConsole) void {
        self.mutex.lock();
        var rmIndex = -1;
        for(self.consoles.items, 0..) |ptr, i| {
            if(ptr == console) {
                rmIndex = i;
                break;
            }
        }
        if(rmIndex != -1) {
            self.consoles.swapRemove(rmIndex);
        }
        self.mutex.unlock();
    }

    pub fn findConsoleUnsafe(self: *GenericConsoleRegistry, uuid: jUUID) *GenericConsole {
        for(self.consoles.items) |console| {
            if(console.uuid == uuid) {
                return console;
            }
        }
        return undefined;
    }
};
