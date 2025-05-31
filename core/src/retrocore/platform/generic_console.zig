const std = @import("std");
const shared = @import("shared");
const LibRetroCore = @import("../sys/libretro.zig").LibRetroCore;
const GenericShared = shared.GenericShared;

const FRAME_SIZE = 1920; // 40ms at 48kHz
const SAVE_DELAY_SECONDS = 30;

var g_instance: ?*LibRetroCore = null;
var g_audio_buffer: std.ArrayList(i16) = undefined;
var g_allocator: std.mem.Allocator = undefined;

pub fn load(allocator: std.mem.Allocator, shared_data: *GenericShared, core_path: []const u8, rom_path: []const u8, save_path: []const u8) !i32 {
    g_allocator = allocator;
    g_audio_buffer = std.ArrayList(i16).init(allocator);
    defer g_audio_buffer.deinit();

    const core_dir = std.fs.path.dirname(core_path) orelse ".";

    var core = try LibRetroCore.init(allocator, core_path, core_dir);
    defer core.deinit();
    g_instance = &core;

    // Set callbacks
    core.setVideoFrameCallback(videoCallback);
    core.setAudioCallback(audioCallback);
    core.setInputCallback(inputCallback);

    // Load core and ROM
    core.loadCore() catch {
        std.debug.print("[RetroGamingCore] Failed to load core\n", .{});
        shared_data.shutdownCompleted = true;
        return 1;
    };

    core.loadROM(rom_path) catch {
        std.debug.print("[RetroGamingCore] Failed to load ROM\n", .{});
        shared_data.shutdownCompleted = true;
        return 1;
    };

    // Load save file
    _ = core.loadSaveFile(save_path) catch false;

    std.debug.print("[RetroGamingCore] Starting generic core\n", .{});

    // Start save loop thread
    const SaveContext = struct {
        shared_ptr: *GenericShared,
        save_path_ptr: []const u8,
        core_ptr: *LibRetroCore,
    };

    const save_context = SaveContext{
        .shared_ptr = shared_data,
        .save_path_ptr = save_path,
        .core_ptr = &core,
    };

    const thread = try std.Thread.spawn(.{}, saveLoop, .{save_context});
    thread.detach();

    // Run main core loop
    core.runCore() catch {};
    shared_data.shutdownCompleted = true;
    return 0;
}

fn videoCallback(data: []const u8, width: u32, height: u32, pitch: usize) void {
    const shared_data = getSharedData() orelse return;

    for (0..height) |y| {
        for (0..width) |x| {
            const pixel_offset = y * pitch + x * 2;
            if (pixel_offset + 1 < data.len) {
                const rgb565: u16 = @as(u16, data[pixel_offset]) | (@as(u16, data[pixel_offset + 1]) << 8);
                const idx = y * width + x;
                if (idx < shared_data.display.len) {
                    shared_data.display[idx] = rgb565;
                }
            }
        }
    }
    shared_data.displayChanged = true;
}

fn audioCallback(data: []const i16) void {
    const shared_data = getSharedData() orelse return;

    // Add samples to buffer
    for (data) |sample| {
        g_audio_buffer.append(sample) catch return;
    }

    if (shared_data.audioChanged) return;

    // Process frames
    while (g_audio_buffer.items.len >= FRAME_SIZE) {
        for (0..FRAME_SIZE) |i| {
            shared_data.audio[i] = g_audio_buffer.orderedRemove(0);
        }
        shared_data.audioSize = FRAME_SIZE;
        shared_data.audioChanged = true;
    }
}

fn inputCallback(port: u32, id: u32) i16 {
    const shared_data = getSharedData() orelse return 0;
    if (port >= shared_data.controls.len) return 0;

    const mask: i16 = @as(i16, 1) << @intCast(id);
    return if (shared_data.controls[port] & mask != 0) 0x7FFF else 0;
}

fn getSharedData() ?*GenericShared {
    // In a real implementation, you'd store a reference to shared_data
    // For now, we'll use a global or pass it through context
    return null; // Placeholder - needs proper shared memory access
}

fn saveLoop(context: anytype) void {
    const delay_ns = SAVE_DELAY_SECONDS * std.time.ns_per_s;
    var next_auto_save = std.time.nanoTimestamp() + delay_ns;
    const check_delay_ns = 10 * std.time.ns_per_ms;
    var next_check = std.time.nanoTimestamp() + check_delay_ns;

    while (!context.shared_ptr.shutdownCompleted) {
        const now = std.time.nanoTimestamp();
        var saved = false;

        if (next_auto_save < now) {
            _ = context.core_ptr.saveSaveFile(context.save_path_ptr) catch false;
            saved = true;
            next_auto_save += delay_ns;
        }

        if (next_check < now) {
            if (context.shared_ptr.shutdownRequested) {
                if (!saved) {
                    _ = context.core_ptr.saveSaveFile(context.save_path_ptr) catch false;
                }
                context.shared_ptr.shutdownCompleted = true;
                break;
            }
            next_check += check_delay_ns;
        }

        std.time.sleep(@intCast(@max(0, next_check - now)));
    }
}
