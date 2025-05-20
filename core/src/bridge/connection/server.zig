
const std = @import("std");
const jni = @import("jni");
const net = @import("network_definitions.zig");
const enet = net.enet;

const jUUID = @import("../util/native_util.zig").jUUID;
const NativeDisplay = @import("../util/native_display.zig").NativeDisplay;
const Int8ArrayPacket = net.Int8ArrayPacket;

// Todo: JNI

// Source
pub const RetroServerClient = struct {
    peer: [*c]enet.ENetPeer,
    authenticated: bool = false,
};

pub const RetroServer = struct {
    enet_mutex: std.Thread.Mutex = .{},
    server: [*c]enet.ENetHost = null,

    mutex: std.Thread.Mutex = .{},
    running: bool = false,
    runningLoops: i32 = 0,
    
    clients: std.ArrayList(RetroServerClient) = std.ArrayList(RetroServerClient).init(std.heap.c_allocator),
    tokens: std.ArrayList([32]u8) = std.ArrayList([32]u8).init(std.heap.c_allocator),

    bytesIn: u64 = 0,
    bytesOut: u64 = 0,

    fn init(port: i32, maxClients: i32) RetroServer {
        var server: RetroServer = .{};
        server.mutex.lock();
        defer server.mutex.unlock();
        server.enet_mutex.lock();
        defer server.enet_mutex.unlock();

        std.debug.print("[RetroServer] Starting ENet server on port {d}", .{port});
        if(enet.enet_initialize() != 0) {
            std.debug.print("[RetroServer] Failed to initialize ENet", .{});
            return server;
        }
        var address: enet.ENetAddress = .{};
        address.host = enet.ENET_HOST_ANY;
        address.port = port;

        server.client = enet.enet_host_create(&address, @intCast(maxClients), 2, 0, 0);
        if(server.client == null) {
            std.debug.print("[RetroServer] Failed to create ENet client", .{});
            enet.enet_deinitialize();
            return server;
        }

        server.peer = enet.enet_host_connect(server.client, &address, 2, 0);
        if(server.peer == null) {
            std.debug.print("[RetroServer] Failed to create ENet server", .{});
            enet.enet_deinitialize();
            return server;
        }

        server.running = true;
        //const mainLoopThread = try std.Thread.spawn(.{}, mainReceiverLoop, .{&server});
        const bandwidthThread = try std.Thread.spawn(.{}, bandwidthMonitorLoop, .{&server});
        //mainLoopThread.detach();
        bandwidthThread.detach();
        std.debug.print("[RetroServer] Started ENet server on port {d}", .{port});
        return server;
    }

    fn dispose(self: *RetroServer) !void {
        self.mutex.lock();
        self.running = false;
        self.mutex.unlock();

        while (true) {
            self.mutex.lock();
            if(self.runningLoops == 0)
                break;
            self.mutex.unlock();
            try std.Thread.yield();
        }

        self.enet_mutex.lock();
        defer self.enet_mutex.unlock();
        if(self.server != null) {
            enet.enet_host_destroy(self.server);
            self.server = null;
        }
        enet.enet_deinitialize();
        self.mutex.unlock();
    }

    fn genToken(self: *RetroServer) [32]u8 {
        var data: [32]u8 = std.mem.zeroes([32]u8);
        @import("../util/native_util.zig").GenerateID(&data) catch {
            std.debug.panic("[RetroServer] Failed to generate secure token; Panic", .{});
        };
        self.mutex.lock();
        defer self.mutex.unlock();
        self.tokens.append(data) catch {
            std.debug.print("[RetroServer] Failed to register generated token!", .{});
        };
        return data;
    }

    fn bandwidthMonitorLoop(self: *RetroServer) void {
        const interval = 5 * std.time.ns_per_s;
        var nextTime = std.time.nanoTimestamp();
        var lastBytesIn: u64 = 0;
        var lastBytesOut: u64 = 0;

        self.mutex.lock();
        self.runningLoops += 1;
        self.mutex.unlock();

        while (true) {
            std.Thread.sleep(@intCast(nextTime - std.time.nanoTimestamp()));

            self.mutex.lock();
            std.debug.print("[RetroServer] Bandwidth: IN: {d} kbps, OUT: {d} kbps", .{
                (self.bytesIn - lastBytesIn) * 8 / 1000 / 5,
                (self.bytesOut - lastBytesOut) * 8 / 1000 / 5
            });
            lastBytesIn = self.bytesIn;
            lastBytesOut = self.bytesOut;

            if(!self.running)
                break;
            self.mutex.unlock();

            nextTime += interval;
        }
        self.runningLoops -= 1;
        self.mutex.unlock();
    }
};
