const std = @import("std");
const jni = @import("jni");
const VideoEncoderInt16 = @import("../codec/video_encoder.zig").VideoEncoderInt16;
const native = @import("../util/native_util.zig");
const shm = @import("shared_memory");
const GenericShared = @import("shared").GenericShared;

var consoleRegistry: ?@import("generic_console_registry.zig").GenericConsoleRegistry = null;

// JNI
pub fn init(_: *jni.cEnv, _: jni.jclass, jUuid: jni.jlong, width: jni.jint, height: jni.jint, sampleRate: jni.jint) callconv(.C) jni.jlong {
    const uuid: *native.jUUID = @ptrFromInt(@as(usize, @intCast(jUuid)));
    const console = GenericConsole.init(width, height, sampleRate, uuid.*) catch |err| {
        std.debug.panic("Failed to init console! Panicking... {any}", .{err});
    };
    return @intCast(@intFromPtr(&console));
}

pub fn start(env: *jni.cEnv, _: jni.jclass, ptr: jni.jlong, retroCore: jni.jstring, core: jni.jstring, rom: jni.jstring, save: jni.jstring) callconv(.C) void {
    const console: *GenericConsole = @ptrFromInt(@as(usize, @intCast(ptr)));

    const retroCoreChars = env.*.*.GetStringUTFChars.?(env, retroCore, null);
    const coreChars = env.*.*.GetStringUTFChars.?(env, core, null);
    const romChars = env.*.*.GetStringUTFChars.?(env, rom, null);
    const saveChars = env.*.*.GetStringUTFChars.?(env, save, null);

    defer {
        env.*.*.ReleaseStringUTFChars.?(env, retroCore, retroCoreChars);
        env.*.*.ReleaseStringUTFChars.?(env, core, coreChars);
        env.*.*.ReleaseStringUTFChars.?(env, rom, romChars);
        env.*.*.ReleaseStringUTFChars.?(env, save, saveChars);
    }

    const retroCoreSlice = std.mem.span(retroCoreChars);
    const coreSlice = std.mem.span(coreChars);
    const romSlice = std.mem.span(romChars);
    const saveSlice = std.mem.span(saveChars);

    console.*.load(retroCoreSlice, coreSlice, romSlice, saveSlice) catch |err| {
        std.debug.panic("Failed to start console! Panicking... {any}", .{err});
    };
}

pub fn stop(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong) callconv(.C) void {
    const console: *GenericConsole = @ptrFromInt(@as(usize, @intCast(ptr)));
    console.dispose() catch |err| {
        std.debug.panic("Failed to stop console! Panicking... {any}", .{err});
    };
}

pub fn getWidth(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong) callconv(.C) jni.jint {
    const console: *GenericConsole = @ptrFromInt(@as(usize, @intCast(ptr)));
    return console.width;
}

// Source
pub const GenericConsole = struct {
    allocator: std.heap.ArenaAllocator = std.heap.ArenaAllocator.init(std.heap.page_allocator),
    mutex: std.Thread.Mutex = .{},
    videoEncoder: ?VideoEncoderInt16 = null,
    sharedMemory: ?shm.SharedMemory(GenericShared) = null,
    childProcess: ?std.process.Child = null,
    uuid: native.jUUID,

    id: [32]u8,
    width: i32,
    height: i32,
    sampleRate: i32,

    pub fn init(width: i32, height: i32, sampleRate: i32, uuid: native.jUUID) !GenericConsole {
        var id: [32]u8 = std.mem.zeroes([32]u8);
        try native.GenerateID(&id);
        var console: GenericConsole = .{
            .uuid = uuid,
            .id = id,
            .width = width,
            .height = height,
            .sampleRate = sampleRate,
        };
        if (consoleRegistry == null) {
            consoleRegistry = @import("generic_console_registry.zig").registryInstance;
        }
        try consoleRegistry.?.register(&console);
        return console;
    }

    pub fn load(self: *GenericConsole, retroCore: []const u8, core: []const u8, rom: []const u8, save: []const u8) !void {
        self.mutex.lock();
        self.sharedMemory = try shm.SharedMemory(GenericShared).create(&self.id, self.allocator.allocator());
        std.debug.print("[RetroGamingCore] Constructed shared memory {s}", .{self.id});
        self.childProcess = std.process.Child.init(&[_][]const u8{
            retroCore,
            "gn",
            &self.id,
            core,
            rom,
            save,
        }, self.allocator.allocator());
        try self.childProcess.?.spawn();
        std.debug.print("[RetroGamingCore] Spawned core process for {s}", .{self.id});
        self.mutex.unlock();
    }

    pub fn dispose(self: *GenericConsole) !void {
        std.debug.print("[RetroGamingCore] Disposing bridge instance {s}", .{self.id});
        consoleRegistry.?.unregister(self);
        self.mutex.lock();
        var handleBackup = self.sharedMemory;
        self.sharedMemory = null;
        if (handleBackup != null) {
            handleBackup.?.data.shutdownRequested = true;
            while (!handleBackup.?.data.shutdownCompleted) {
                try std.Thread.yield();
            }
            handleBackup.?.close();
            std.debug.print("[RetroGamingCore] Emulator shutdown completed", .{});
        }
        _ = try self.childProcess.?.kill();
        self.mutex.unlock();
        self.allocator.deinit();
    }

    pub fn createFrame(self: *GenericConsole) !std.ArrayList(u8) {
        if (self.videoEncoder == null) {
            self.videoEncoder = VideoEncoderInt16.init(self.width, self.height) catch {
                std.debug.print("[RetroGamingCore] Failed to initialize video encoder", .{});
                return;
            };
        }
        var curr = std.ArrayList(i16).init(std.heap.page_allocator);
        try curr.appendSlice(@ptrCast(self.sharedMemory.?.data.display[0..@intCast(self.width * self.height)]));
        const encoded = try self.videoEncoder.?.encodeFrame(curr);
        curr.deinit();
        return encoded;
    }

    pub fn input(self: *GenericConsole, port: usize, data: i16) void {
        if (self.sharedMemory) |handle| {
            handle.data.controls[port] = data;
        }
    }
};
