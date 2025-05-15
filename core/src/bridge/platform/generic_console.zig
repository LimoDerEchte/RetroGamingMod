
const std = @import("std");
const VideoEncoderInt16 = @import("../codec/video_encoder.zig").VideoEncoderInt16;
const native = @import("../util/native_util.zig");
const shm = @import("shared_memory");
const GenericShared = @import("shared").GenericShared;

const consoleRegistry = @import("generic_console_registry.zig").registryInstance;

pub const GenericConsole = struct {
    mutex: std.Thread.Mutex = .{},
    videoEncoder: ?VideoEncoderInt16 = undefined,
    sharedMemory: ?shm.SharedMemory(GenericShared) = undefined,
    childProcess: ?std.process.Child = undefined,
    uuid: native.jUUID,

    id: [32]u8,
    width: i32,
    height: i32,
    sampleRate: i32,

    fn init(width: i32, height: i32, sampleRate: i32, uuid: native.jUUID) GenericConsole {
        var id: [32]u8 = std.mem.zeroes([32]u8);
        native.GenerateID(&id);
        const console: GenericConsole = .{
            .uuid = uuid,
            .id = id,
            .width = width,
            .height = height,
            .sampleRate = sampleRate
        };
        consoleRegistry.register(&console);
        return console;
    }

    fn load(self: *GenericConsole, retroCore: []u8, core: []u8, rom: []u8, save: []u8) !void {
        self.mutex.lock();
        self.sharedMemory = try shm.SharedMemory(GenericShared).create(self.id, std.heap.c_allocator);
        std.debug.print("[RetroGamingCore] Constructed shared memory {s}", .{self.id});
        self.childProcess = std.process.Child.init([]const []const u8 {
            retroCore,
            "gn",
            self.id,
            core,
            rom,
            save
        }, std.heap.c_allocator);
        try self.childProcess.?.spawn();
        std.debug.print("[RetroGamingCore] Spawned core process for {s}", .{self.id});
        self.mutex.unlock();
    }

    fn dispose(self: *GenericConsole) !void {
        std.debug.print("[RetroGamingCore] Disposing bridge instance {s}", .{self.id});
        consoleRegistry.unregister(self);
        self.mutex.lock();
        const handleBackup = self.sharedMemory;
        self.sharedMemory = undefined;
        if(handleBackup) |handle| {
            handle.data.shutdownRequested = true;
            while (!handle.data.shutdownCompleted) {
                std.Thread.yield();
            }
            handle.close();
            std.debug.print("[RetroGamingCore] Emulator shutdown completed", .{});
        }
        try self.childProcess.?.kill();
    }

    fn createFrame(self: *GenericConsole) !std.ArrayList(u8) {
        if(self.videoEncoder == undefined) {
            self.videoEncoder = VideoEncoderInt16.init(self.width, self.height);
        }
        const curr = std.ArrayList(i16).init(std.heap.c_allocator);
        curr.appendSlice(self.sharedMemory.?.data.display[0..self.width * self.height]);
        return self.videoEncoder.?.encodeFrame(curr);
    }

    fn input(self: *GenericConsole, port: i32, data: i16) void {
        if(self.sharedMemory) |handle| {
            handle.data.controls[port] = data;
        }
    }
};
