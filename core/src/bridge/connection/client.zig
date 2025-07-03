const std = @import("std");
const jni = @import("jni");
const net = @import("network_definitions.zig");
const enet = net.enet;

const jUUID = @import("../util/native_util.zig").jUUID;
const NativeDisplay = @import("../util/native_display.zig").NativeDisplay;
const Int8ArrayPacket = net.Int8ArrayPacket;

// JNI
pub fn connect(env: *jni.cEnv, _: jni.jclass, ip: jni.jstring, port: jni.jint, token: jni.jstring) callconv(.C) jni.jlong {
    const client = RetroClient.init(env.*.*.GetStringUTFChars.?(env, ip, null), @intCast(port), env.*.*.GetStringUTFChars.?(env, token, null)) catch {
        std.debug.panic("[RetroServer] Failed to create retro client; Panic\n", .{});
    };
    return @intCast(@intFromPtr(&client));
}

pub fn disconnect(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong) callconv(.C) void {
    const client: *RetroClient = @ptrFromInt(@as(usize, @intCast(ptr)));
    client.dispose() catch {
        std.debug.print("[RetroClient] Something went wrong while disposing client!\n", .{});
    };
}

pub fn isAuthenticated(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong) callconv(.C) jni.jboolean {
    const client: *RetroClient = @ptrFromInt(@as(usize, @intCast(ptr)));
    return jni.boolToJboolean(client.authenticated);
}

pub fn registerScreen(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong, jUuid: jni.jlong, width: jni.jint, height: jni.jint, sampleRate: jni.jint) callconv(.C) jni.jlong {
    const client: *RetroClient = @ptrFromInt(@as(usize, @intCast(ptr)));
    const uuid: *jUUID = @ptrFromInt(@as(usize, @intCast(jUuid)));

    var display = NativeDisplay.init(width, height);
    client.registerDisplay(uuid, &display, sampleRate) catch {
        std.debug.print("[RetroClient] Something went wrong while registering display!\n", .{});
    };
    return @intCast(@intFromPtr(&display));
}

pub fn unregisterScreen(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong, jUuid: jni.jlong) callconv(.C) void {
    const client: *RetroClient = @ptrFromInt(@as(usize, @intCast(ptr)));
    const uuid: *jUUID = @ptrFromInt(@as(usize, @intCast(jUuid)));
    client.unregisterDisplay(uuid);
}

pub fn sendControlUpdate(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong, jUuid: jni.jlong, port: jni.jint, data: jni.jshort) callconv(.C) void {
    const client: *RetroClient = @ptrFromInt(@as(usize, @intCast(ptr)));
    const uuid: *jUUID = @ptrFromInt(@as(usize, @intCast(jUuid)));
    client.sendControlsUpdate(uuid, port, data) catch {
        std.debug.print("[RetroClient] Something went wrong while sending controls update!\n", .{});
    };
}

pub fn updateAudioDistance(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong, ptrUuid: jni.jlong, distance: jni.jdouble) callconv(.C) void {
    _ = ptr;
    _ = ptrUuid;
    _ = distance;
}

// Source
pub const RetroClient = struct {
    enet_mutex: std.Thread.Mutex = .{},
    client: [*c]enet.ENetHost = null,
    peer: [*c]enet.ENetPeer = null,

    mutex: std.Thread.Mutex = .{},
    displays: std.AutoHashMap(i64, *NativeDisplay) = std.AutoHashMap(i64, *NativeDisplay).init(std.heap.page_allocator),

    running: bool = false,
    runningLoops: i32 = 0,
    authenticated: bool = false,
    token: [*c]const u8,

    bytesIn: u64 = 0,
    bytesOut: u64 = 0,

    fn init(ip: [*c]const u8, port: u16, token: [*c]const u8) !RetroClient {
        var client: RetroClient = .{ .token = token };
        client.mutex.lock();
        defer client.mutex.unlock();
        client.enet_mutex.lock();
        defer client.enet_mutex.unlock();

        std.debug.print("[RetroClient] Connecting to ENet server on {s}:{d}\n", .{ ip, port });
        if (enet.enet_initialize() != 0) {
            std.debug.print("[RetroClient] Failed to initialize ENet\n", .{});
            return client;
        }
        var address: enet.ENetAddress = .{};
        _ = enet.enet_address_set_host(&address, ip);
        address.port = port;

        client.client = enet.enet_host_create(&address, 1, 2, 0, 0);
        if (client.client == null) {
            std.debug.print("[RetroClient] Failed to create ENet client\n", .{});
            enet.enet_deinitialize();
            return client;
        }

        client.peer = enet.enet_host_connect(client.client, &address, 2, 0);
        if (client.peer == null) {
            std.debug.print("[RetroClient] Failed to connect ENet client\n", .{});
            enet.enet_deinitialize();
            return client;
        }

        client.running = true;
        const mainLoopThread = try std.Thread.spawn(.{}, mainLoop, .{&client});
        const bandwidthThread = try std.Thread.spawn(.{}, bandwidthMonitorLoop, .{&client});
        mainLoopThread.detach();
        bandwidthThread.detach();
        return client;
    }

    fn dispose(self: *RetroClient) !void {
        self.mutex.lock();
        self.running = false;
        self.mutex.unlock();

        while (true) {
            self.mutex.lock();
            if (self.runningLoops == 0)
                break;
            self.mutex.unlock();
            try std.Thread.yield();
        }

        self.enet_mutex.lock();
        defer self.enet_mutex.unlock();
        if (self.client != null) {
            enet.enet_host_destroy(self.client);
            self.client = null;
        }
        enet.enet_deinitialize();
        self.displays.deinit();
        self.mutex.unlock();
    }

    fn registerDisplay(self: *RetroClient, uuid: *jUUID, display: *NativeDisplay, _: i32) !void {
        self.mutex.lock();
        defer self.mutex.unlock();
        try self.displays.put(uuid.combine(), display);
    }

    fn unregisterDisplay(self: *RetroClient, uuid: *jUUID) void {
        self.mutex.lock();
        defer self.mutex.unlock();
        _ = self.displays.remove(uuid.combine());
    }

    fn sendControlsUpdate(self: *RetroClient, uuid: *jUUID, port: i32, data: i16) !void {
        self.mutex.lock();
        defer self.mutex.unlock();
        var packet: Int8ArrayPacket = .{
            .type = net.PacketType.PACKET_UPDATE_CONTROLS,
            .ref = uuid,
        };
        try packet.data.append(@intCast(port));
        var intData: [2]u8 = std.mem.zeroes([2]u8);
        std.mem.writeInt(i16, &intData, data, std.builtin.Endian.little);
        try packet.data.appendSlice(&intData);
        self.enet_mutex.lock();
        defer self.enet_mutex.unlock();
        _ = enet.enet_peer_send(self.peer, 0, try packet.pack());
        self.bytesOut += 28;
    }

    fn mainLoop(self: *RetroClient) !void {
        if (self.client == null) {
            return;
        }
        self.mutex.lock();
        self.runningLoops += 1;
        self.mutex.unlock();

        while (true) {
            var event: enet.ENetEvent = .{};
            self.enet_mutex.lock();
            const status = enet.enet_host_service(self.client, &event, 0);
            self.enet_mutex.unlock();

            if (status < 0) {
                std.debug.print("[RetroClient] Failed to receive ENet event ({d})\n", .{status});
                continue;
            } else if (status == 0) {
                try std.Thread.yield();
                continue;
            }

            switch (event.type) {
                enet.ENET_EVENT_TYPE_NONE => {
                    std.debug.print("[RetroClient] Event received an ENET_EVENT_TYPE_NONE\n", .{});
                },
                enet.ENET_EVENT_TYPE_CONNECT => {
                    try self.onConnect();
                },
                enet.ENET_EVENT_TYPE_DISCONNECT => {
                    try self.onDisconnect();
                },
                enet.ENET_EVENT_TYPE_RECEIVE => {
                    self.mutex.lock();
                    self.bytesIn += event.packet.*.dataLength;
                    self.mutex.unlock();
                    try self.onMessage(event.packet);
                },
                else => {
                    std.debug.print("[RetroClient] Event received an invalid event type\n", .{});
                },
            }

            self.mutex.lock();
            if (!self.running)
                break;
            self.mutex.unlock();
        }
        self.runningLoops -= 1;
        self.mutex.unlock();
    }

    fn bandwidthMonitorLoop(self: *RetroClient) void {
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
            std.debug.print("[RetroClient] Bandwidth: IN: {d} kbps, OUT: {d} kbps\n", .{ (self.bytesIn - lastBytesIn) * 8 / 1000 / 5, (self.bytesOut - lastBytesOut) * 8 / 1000 / 5 });
            lastBytesIn = self.bytesIn;
            lastBytesOut = self.bytesOut;

            if (!self.running)
                break;
            self.mutex.unlock();

            nextTime += interval;
        }
        self.runningLoops -= 1;
        self.mutex.unlock();
    }

    fn onConnect(self: *RetroClient) !void {
        std.debug.print("[RetroClient] Connection established to {d}\n", .{self.peer.*.address.port});

        self.enet_mutex.lock();
        defer self.enet_mutex.unlock();

        var nullUuid = jUUID{};
        var packet: Int8ArrayPacket = .{ .type = net.PacketType.PACKET_AUTH, .ref = &nullUuid };
        try packet.data.appendSlice(std.mem.span(self.token));
        _ = enet.enet_peer_send(self.peer, 0, try packet.pack());

        self.mutex.lock();
        self.bytesOut += 57;
        self.mutex.unlock();

        std.debug.print("[RetroClient] Authorizing with token {s}\n", .{self.token});
    }

    fn onDisconnect(self: *RetroClient) !void {
        try self.dispose();
    }

    fn onMessage(self: *RetroClient, packet: [*c]enet.ENetPacket) !void {
        if (packet == null) {
            std.debug.print("[RetroClient] Received packet is nullptr\n", .{});
            return;
        }
        if (packet.*.dataLength == 0) {
            std.debug.print("[RetroClient] Received empty packet from server\n", .{});
            return;
        }
        const packetType: net.PacketType = @enumFromInt(packet.*.data.?[0]);
        switch (packetType) {
            net.PacketType.PACKET_AUTH_ACK => {
                self.mutex.lock();
                self.authenticated = true;
                self.mutex.unlock();
                std.debug.print("[RetroClient] Connection token accepted by server\n", .{});
            },
            net.PacketType.PACKET_KEEP_ALIVE => {
                const id: u8 = @intFromEnum(net.PacketType.PACKET_KEEP_ALIVE);
                self.enet_mutex.lock();
                _ = enet.enet_peer_send(self.peer, 0, enet.enet_packet_create(&id, 1, enet.ENET_PACKET_FLAG_RELIABLE));
                self.enet_mutex.unlock();
                self.mutex.lock();
                self.bytesOut += 1;
                self.mutex.unlock();
            },
            net.PacketType.PACKET_KICK => {
                const parsed = try Int8ArrayPacket.unpack(packet);
                std.debug.print("[RetroClient] Received kick packet: {s}\n", .{parsed.data.items});
            },
            net.PacketType.PACKET_UPDATE_DISPLAY => {
                var parsed = try Int8ArrayPacket.unpack(packet);
                self.mutex.lock();
                const display = self.displays.get(parsed.ref.combine());
                try display.?.receive(&parsed.data);
                self.mutex.unlock();
            },
            net.PacketType.PACKET_AUTH, net.PacketType.PACKET_UPDATE_CONTROLS => {
                std.debug.print("[RetroClient] Received C2S packet on client\n", .{});
            },
            else => {
                std.debug.print("[RetroClient] Unknown C2S packet type {d}\n", .{packet.*.data.?[0]});
            },
        }
    }
};
