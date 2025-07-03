const std = @import("std");
const jni = @import("jni");
const net = @import("network_definitions.zig");
const enet = net.enet;

const jUUID = @import("../util/native_util.zig").jUUID;
const NativeDisplay = @import("../util/native_display.zig").NativeDisplay;
const Int8ArrayPacket = net.Int8ArrayPacket;

var consoleRegistry: ?@import("../platform/generic_console_registry.zig").GenericConsoleRegistry = null;

// JNI
pub fn startServer(_: *jni.cEnv, _: jni.jclass, port: jni.jint, maxClients: i32) callconv(.C) jni.jlong {
    const client = RetroServer.init(@intCast(port), maxClients) catch {
        std.debug.panic("[RetroServer] Failed to create retro server; Panic\n", .{});
    };
    return @intCast(@intFromPtr(&client));
}

pub fn stopServer(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong) callconv(.C) void {
    const server: *RetroServer = @ptrFromInt(@as(usize, @intCast(ptr)));
    server.dispose() catch {
        std.debug.print("[RetroServer] Something went wrong while disposing server!\n", .{});
    };
}

pub fn requestToken(env: *jni.cEnv, _: jni.jclass, ptr: jni.jlong) callconv(.C) jni.jstring {
    const server: *RetroServer = @ptrFromInt(@as(usize, @intCast(ptr)));
    std.debug.print("DBG 1\n", .{});

    const token_str = server.allocator.allocator().alloc(u8, 33) catch {
        std.debug.panic("[RetroServer] Failed to allocate secure token; Panic\n", .{});
    };
    std.debug.print("DBG 2\n", .{});
    defer server.allocator.allocator().free(token_str);
    std.debug.print("DBG 3\n", .{});

    var token = server.genToken() catch {
        std.debug.panic("[RetroServer] Failed to allocate raw token; Panic\n", .{});
    };
    std.debug.print("DBG 4\n", .{});
    std.mem.copyForwards(u8, token_str[0..32], &token);
    std.debug.print("DBG 5\n", .{});
    token_str[32] = 0;
    std.debug.print("DBG 6\n", .{});

    return env.*.*.NewStringUTF.?(env, @ptrCast(token_str));
}

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

    allocator: std.heap.ArenaAllocator = std.heap.ArenaAllocator.init(std.heap.page_allocator),
    clients: std.ArrayList(RetroServerClient) = std.ArrayList(RetroServerClient).init(std.heap.page_allocator),
    tokens: std.ArrayList([32]u8) = std.ArrayList([32]u8).init(std.heap.page_allocator),

    bytesIn: u64 = 0,
    bytesOut: u64 = 0,

    fn init(port: u16, maxClients: i32) !RetroServer {
        if (consoleRegistry == null) {
            consoleRegistry = @import("../platform/generic_console_registry.zig").registryInstance;
        }

        var server: RetroServer = .{};
        server.mutex.lock();
        defer server.mutex.unlock();
        server.enet_mutex.lock();
        defer server.enet_mutex.unlock();

        std.debug.print("[RetroServer] Starting ENet server on port {d}\n", .{port});
        if (enet.enet_initialize() != 0) {
            std.debug.print("[RetroServer] Failed to initialize ENet\n", .{});
            return server;
        }
        var address: enet.ENetAddress = .{};
        address.host = enet.ENET_HOST_ANY;
        address.port = port;

        server.server = enet.enet_host_create(&address, @intCast(maxClients), 2, 0, 0);
        if (server.server == null) {
            std.debug.print("[RetroServer] Failed to create ENet client\n", .{});
            enet.enet_deinitialize();
            return server;
        }

        server.running = true;
        const mainLoopThread = try std.Thread.spawn(.{}, mainReceiverLoop, .{&server});
        const bandwidthThread = try std.Thread.spawn(.{}, bandwidthMonitorLoop, .{&server});
        const keepAliveThread = try std.Thread.spawn(.{}, mainKeepAliveLoop, .{&server});
        const videoSenderThread = try std.Thread.spawn(.{}, videoSenderLoop, .{ &server, 30 });
        mainLoopThread.detach();
        bandwidthThread.detach();
        keepAliveThread.detach();
        videoSenderThread.detach();
        std.debug.print("[RetroServer] Started ENet server on port {d}\n", .{port});
        return server;
    }

    fn dispose(self: *RetroServer) !void {
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
        if (self.server != null) {
            enet.enet_host_destroy(self.server);
            self.server = null;
        }
        enet.enet_deinitialize();

        self.allocator.deinit();
        self.clients.deinit();
        self.tokens.deinit();
        self.mutex.unlock();
    }

    fn genToken(self: *RetroServer) ![32]u8 {
        var data: [32]u8 = (try self.allocator.allocator().alloc([32]u8, 1))[0];
        @import("../util/native_util.zig").GenerateID(&data) catch {
            std.debug.panic("[RetroServer] Failed to generate secure token; Panic\n", .{});
        };
        self.mutex.lock();
        defer self.mutex.unlock();
        self.tokens.append(data) catch {
            std.debug.print("[RetroServer] Failed to register generated token!\n", .{});
        };
        return data;
    }

    fn mainReceiverLoop(self: *RetroServer) !void {
        if (self.server == null) {
            return;
        }
        self.mutex.lock();
        self.runningLoops += 1;
        self.mutex.unlock();

        while (true) {
            var event: enet.ENetEvent = .{};
            self.enet_mutex.lock();
            const status = enet.enet_host_service(self.server, &event, 0);
            self.enet_mutex.unlock();

            if (status < 0) {
                std.debug.print("[RetroServer] Failed to receive ENet event ({d})\n", .{status});
                continue;
            } else if (status == 0) {
                try std.Thread.yield();
                continue;
            }

            switch (event.type) {
                enet.ENET_EVENT_TYPE_NONE => {
                    std.debug.print("[RetroServer] Event received an ENET_EVENT_TYPE_NONE\n", .{});
                },
                enet.ENET_EVENT_TYPE_CONNECT => {
                    try self.onConnect(event.peer);
                },
                enet.ENET_EVENT_TYPE_DISCONNECT => {
                    self.onDisconnect(event.peer);
                },
                enet.ENET_EVENT_TYPE_RECEIVE => {
                    self.mutex.lock();
                    self.bytesIn += event.packet.*.dataLength;
                    self.mutex.unlock();
                    self.onMessage(event.peer, event.packet);
                },
                else => {
                    std.debug.print("[RetroServer] Event received an invalid event type\n", .{});
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

    fn bandwidthMonitorLoop(self: *RetroServer) void {
        const interval = 5 * std.time.ns_per_s;
        var nextTime = std.time.nanoTimestamp();
        var lastBytesIn: u64 = 0;
        var lastBytesOut: u64 = 0;

        self.mutex.lock();
        self.runningLoops += 1;
        self.mutex.unlock();

        while (true) {
            if (nextTime - std.time.nanoTimestamp() > 0)
                std.Thread.sleep(@intCast(nextTime - std.time.nanoTimestamp()));

            self.mutex.lock();
            std.debug.print("[RetroServer] Bandwidth: IN: {d} kbps, OUT: {d} kbps\n", .{ (self.bytesIn - lastBytesIn) * 8 / 1000 / 5, (self.bytesOut - lastBytesOut) * 8 / 1000 / 5 });
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

    fn mainKeepAliveLoop(self: *RetroServer) void {
        const interval = 5 * std.time.ns_per_s;
        var nextTime = std.time.nanoTimestamp();

        self.mutex.lock();
        self.runningLoops += 1;
        self.mutex.unlock();

        while (true) {
            if (nextTime - std.time.nanoTimestamp() > 0)
                std.Thread.sleep(@intCast(nextTime - std.time.nanoTimestamp()));

            self.mutex.lock();
            const id: u8 = @intFromEnum(net.PacketType.PACKET_KEEP_ALIVE);
            for (self.clients.items) |client| {
                if (client.peer == null or client.peer.*.state != enet.ENET_PEER_STATE_CONNECTED) {
                    continue;
                }
                self.enet_mutex.lock();
                _ = enet.enet_peer_send(client.peer, 0, enet.enet_packet_create(&id, 1, enet.ENET_PACKET_FLAG_RELIABLE));
                self.enet_mutex.unlock();
                self.bytesOut += 1;
            }

            if (!self.running)
                break;
            self.mutex.unlock();

            nextTime += interval;
        }
        self.runningLoops -= 1;
        self.mutex.unlock();
    }

    fn videoSenderLoop(self: *RetroServer, fps: i32) void {
        const interval = @divFloor(std.time.ns_per_s, fps);
        var nextTime = std.time.nanoTimestamp();

        self.mutex.lock();
        self.runningLoops += 1;
        self.mutex.unlock();

        while (true) {
            if (nextTime - std.time.nanoTimestamp() > 0)
                std.Thread.sleep(@intCast(nextTime - std.time.nanoTimestamp()));

            consoleRegistry.?.mutex.lock();
            for (consoleRegistry.?.consoles.items) |console| {
                if (!console.sharedMemory.?.data.displayChanged)
                    continue;
                const frame = console.*.createFrame() catch {
                    continue;
                };
                if (frame.items.len == 0) {
                    continue;
                }
                var packet: Int8ArrayPacket = .{
                    .type = net.PacketType.PACKET_UPDATE_DISPLAY,
                    .ref = &console.*.uuid,
                    .data = frame,
                };
                const pack = packet.pack() catch {
                    continue;
                };
                self.mutex.lock();
                for (self.clients.items) |client| {
                    if (client.peer == null or client.peer.*.state != enet.ENET_PEER_STATE_CONNECTED) {
                        continue;
                    }
                    self.enet_mutex.lock();
                    _ = enet.enet_peer_send(client.peer, 0, pack);
                    self.enet_mutex.unlock();
                    self.bytesOut += pack.*.dataLength;
                }
                self.mutex.unlock();
            }
            consoleRegistry.?.mutex.unlock();

            self.mutex.lock();
            if (!self.running)
                break;
            self.mutex.unlock();

            nextTime += interval;
        }
        self.runningLoops -= 1;
        self.mutex.unlock();
    }

    fn onConnect(self: *RetroServer, peer: [*c]enet.ENetPeer) !void {
        self.mutex.lock();
        defer self.mutex.unlock();
        const client: RetroServerClient = .{ .peer = peer };
        try self.clients.append(client);
    }

    fn onDisconnect(self: *RetroServer, peer: [*c]enet.ENetPeer) void {
        self.mutex.lock();
        defer self.mutex.unlock();
        for (self.clients.items, 0..) |client, i| {
            if (client.peer != peer)
                continue;
            _ = self.clients.swapRemove(i);
            break;
        }
    }

    fn onMessage(self: *RetroServer, peer: [*c]enet.ENetPeer, packet: [*c]enet.ENetPacket) void {
        if (packet == null) {
            std.debug.print("[RetroServer] Received packet is nullptr\n", .{});
            return;
        }
        if (packet.*.dataLength == 0) {
            std.debug.print("[RetroServer] Received empty packet from client\n", .{});
            return;
        }
        self.mutex.lock();
        defer self.mutex.unlock();
        var client = self.findClientByPeerUnsafe(peer);
        const packetType: net.PacketType = @enumFromInt(packet.*.data.?[0]);
        if (packetType != net.PacketType.PACKET_AUTH and !client.?.authenticated) {
            std.debug.print("[RetroServer] Received non-auth packet before auth from {any}\n", .{peer});
            return;
        }
        switch (packetType) {
            net.PacketType.PACKET_AUTH => {
                if (client.?.authenticated) {
                    std.debug.print("[RetroServer] Received auth packet after auth from {any}\n", .{peer});
                    return;
                }
                for (self.tokens.items, 0..) |token, i| {
                    if (!std.mem.eql(u8, token[0..32], packet.*.data[1..33]))
                        continue;
                    client.?.authenticated = true;
                    const id: u8 = @intFromEnum(net.PacketType.PACKET_AUTH_ACK);
                    self.enet_mutex.lock();
                    _ = enet.enet_peer_send(peer, 0, enet.enet_packet_create(&id, 1, enet.ENET_PACKET_FLAG_RELIABLE));
                    self.enet_mutex.unlock();
                    self.bytesOut += 1;
                    _ = self.tokens.swapRemove(i);
                    break;
                }
                if (client.?.authenticated) {
                    std.debug.print("[RetroServer] Successfully authorized connection {d}\n", .{peer.*.incomingPeerID});
                } else {
                    self.kick(peer, "Invalid token");
                    std.debug.print("[RetroServer] Kicking connection with invalid token ({d})\n", .{peer.*.incomingPeerID});
                }
            },
            net.PacketType.PACKET_UPDATE_CONTROLS => {
                const parsed = Int8ArrayPacket.unpack(packet) catch {
                    std.debug.print("[RetroServer] Failed decoding control update packet!\n", .{});
                    return;
                };
                const port: usize = @intCast(parsed.data.items[0]);
                const data: i16 = std.mem.readInt(i16, parsed.data.items[1..3], std.builtin.Endian.little);

                consoleRegistry.?.mutex.lock();
                defer consoleRegistry.?.mutex.unlock();
                const console = consoleRegistry.?.findConsoleUnsafe(parsed.ref);
                if (console == null) {
                    std.debug.print("[RetroServer] Received control update packet for non existent console!\n", .{});
                }
                console.?.input(port, data);
            },
            net.PacketType.PACKET_KEEP_ALIVE => {
                // Empty block, currently no logic
            },
            net.PacketType.PACKET_AUTH_ACK, net.PacketType.PACKET_KICK, net.PacketType.PACKET_UPDATE_DISPLAY, net.PacketType.PACKET_UPDATE_AUDIO => {
                std.debug.print("[RetroServer] Received S2C packet on server\n", .{});
            },
        }
    }

    fn kick(self: *RetroServer, peer: [*c]enet.ENetPeer, message: []const u8) void {
        var packet: Int8ArrayPacket = .{
            .type = net.PacketType.PACKET_KICK,
            .ref = @constCast(&jUUID.zero()),
            .data = std.ArrayList(u8).fromOwnedSlice(self.allocator.allocator(), @constCast(message)),
        };
        const pack = packet.pack() catch {
            return;
        };
        self.enet_mutex.lock();
        defer self.enet_mutex.unlock();
        _ = enet.enet_peer_send(peer, 0, pack);
        _ = enet.enet_peer_disconnect(peer, 0);
        packet.deinit();
        self.mutex.lock();
        defer self.mutex.unlock();
        self.bytesOut += pack.*.dataLength;
    }

    fn findClientByPeerUnsafe(self: *RetroServer, peer: [*c]enet.ENetPeer) ?RetroServerClient {
        for (self.clients.items) |client| {
            if (client.peer == peer)
                return client;
        }
        return null;
    }
};
