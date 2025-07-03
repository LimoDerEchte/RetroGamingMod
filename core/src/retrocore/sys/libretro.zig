// FILE CONVERTED FROM ORIGINAL CPP CODE USING CLAUDE SONNET 4
// FURTHER ADJUSTMENTS WILL BE MADE IN THE FUTURE AND THIS NOTICE MIGHT BE REMOVED AT SOME POINT

const std = @import("std");
const builtin = @import("builtin");
const print = std.debug.print;
const Allocator = std.mem.Allocator;
const ArrayList = std.ArrayList;
const HashMap = std.HashMap;
const Mutex = std.Thread.Mutex;

// LibRetro C interface
const c = @cImport(@cInclude("libretro.h"));

const LibRetroError = error{
    CoreLoadFailed,
    FunctionLoadFailed,
    RomLoadFailed,
    SaveFileError,
    OutOfMemory,
    InvalidPixelFormat,
};

const PixelFormat = enum(c_uint) {
    rgb565 = c.RETRO_PIXEL_FORMAT_RGB565,
    xrgb1555 = c.RETRO_PIXEL_FORMAT_0RGB1555,
    xrgb8888 = c.RETRO_PIXEL_FORMAT_XRGB8888,
};

const VideoFrameCallback = *const fn (data: []const u8, width: u32, height: u32, pitch: usize) void;
const AudioCallback = *const fn (data: []const i16) void;
const InputCallback = *const fn (port: u32, id: u32) i16;

// Global instance pointer for C callbacks
var g_instance: ?*LibRetroCore = null;
var g_system_info: c.retro_system_info = std.mem.zeroes(c.retro_system_info);
var g_av_info: c.retro_system_av_info = std.mem.zeroes(c.retro_system_av_info);

// Logging function for LibRetro cores
fn logPrintf(level: c.retro_log_level, fmt: [*c]const u8, ...) callconv(.C) void {
    if (builtin.mode == .Debug) {
        // In a real implementation, you'd want proper varargs handling
        // For now, we'll use a simplified approach
        _ = level;
        if (fmt) |f| {
            print("[LibRetroCore] {s}\n", .{f});
        }
    }
}

// Color conversion utilities
fn rgb8888ToRgb565(rgb8888: u32) u16 {
    const red: u8 = @truncate((rgb8888 >> 16) & 0xFF);
    const green: u8 = @truncate((rgb8888 >> 8) & 0xFF);
    const blue: u8 = @truncate(rgb8888 & 0xFF);

    const r5: u16 = (red * 31) / 255;
    const g6: u16 = (green * 63) / 255;
    const b5: u16 = (blue * 31) / 255;

    return (r5 << 11) | (g6 << 5) | b5;
}

fn rgb1555ToRgb565(rgb1555: u16) u16 {
    const red = (rgb1555 >> 10) & 0x1F;
    const green = (rgb1555 >> 5) & 0x1F;
    const blue = rgb1555 & 0x1F;

    const g6 = (green << 1) | (green >> 4);
    return (red << 11) | (g6 << 5) | blue;
}

const EnvVars = struct {
    variables: HashMap([]const u8, []const u8, std.hash_map.StringContext, std.hash_map.default_max_load_percentage),
    updated: bool,
    allocator: Allocator,

    fn init(allocator: Allocator) EnvVars {
        return EnvVars{
            .variables = HashMap([]const u8, []const u8, std.hash_map.StringContext, std.hash_map.default_max_load_percentage).init(allocator),
            .updated = false,
            .allocator = allocator,
        };
    }

    fn deinit(self: *EnvVars) void {
        // Free all stored strings
        var it = self.variables.iterator();
        while (it.next()) |entry| {
            self.allocator.free(entry.key_ptr.*);
            self.allocator.free(entry.value_ptr.*);
        }
        self.variables.deinit();
    }
};

pub const LibRetroCore = struct {
    allocator: Allocator,
    system_path: []const u8,
    core_path: []const u8,
    core_handle: ?*anyopaque,
    save_mutex: Mutex,
    pixel_format: PixelFormat,
    env_vars: EnvVars,

    // Callbacks
    video_frame_callback: ?VideoFrameCallback,
    audio_callback: ?AudioCallback,
    input_callback: ?InputCallback,

    // LibRetro function pointers
    retro_init: ?*const fn () callconv(.C) void,
    retro_deinit: ?*const fn () callconv(.C) void,
    retro_run: ?*const fn () callconv(.C) void,
    retro_load_game: ?*const fn (*const c.retro_game_info) callconv(.C) bool,
    retro_unload_game: ?*const fn () callconv(.C) void,
    retro_set_video_refresh: ?*const fn (*const fn (?*const anyopaque, c_uint, c_uint, usize) callconv(.C) void) callconv(.C) void,
    retro_set_environment: ?*const fn (*const fn (c_uint, ?*anyopaque) callconv(.C) bool) callconv(.C) void,
    retro_set_input_poll: ?*const fn (*const fn () callconv(.C) void) callconv(.C) void,
    retro_set_input_state: ?*const fn (*const fn (c_uint, c_uint, c_uint, c_uint) callconv(.C) i16) callconv(.C) void,
    retro_set_audio_sample: ?*const fn (*const fn (i16, i16) callconv(.C) void) callconv(.C) void,
    retro_set_audio_sample_batch: ?*const fn (*const fn ([*c]const i16, usize) callconv(.C) usize) callconv(.C) void,
    retro_get_system_info: ?*const fn (*c.retro_system_info) callconv(.C) void,
    retro_get_system_av_info: ?*const fn (*c.retro_system_av_info) callconv(.C) void,
    retro_get_memory_data: ?*const fn (c_uint) callconv(.C) ?*anyopaque,
    retro_get_memory_size: ?*const fn (c_uint) callconv(.C) usize,

    pub fn init(allocator: Allocator, core_path: []const u8, system_path: []const u8) !LibRetroCore {
        const core_path_owned = try allocator.dupe(u8, core_path);
        const system_path_owned = try allocator.dupe(u8, system_path);

        var core = LibRetroCore{
            .allocator = allocator,
            .system_path = system_path_owned,
            .core_path = core_path_owned,
            .core_handle = null,
            .save_mutex = Mutex{},
            .pixel_format = .rgb565,
            .env_vars = EnvVars.init(allocator),
            .video_frame_callback = null,
            .audio_callback = null,
            .input_callback = null,
            .retro_init = null,
            .retro_deinit = null,
            .retro_run = null,
            .retro_load_game = null,
            .retro_unload_game = null,
            .retro_set_video_refresh = null,
            .retro_set_environment = null,
            .retro_set_input_poll = null,
            .retro_set_input_state = null,
            .retro_set_audio_sample = null,
            .retro_set_audio_sample_batch = null,
            .retro_get_system_info = null,
            .retro_get_system_av_info = null,
            .retro_get_memory_data = null,
            .retro_get_memory_size = null,
        };

        g_instance = &core;
        return core;
    }

    pub fn deinit(self: *LibRetroCore) void {
        if (self.retro_unload_game) |unload| unload();
        if (self.retro_deinit) |deinit_fn| deinit_fn();

        if (self.core_handle) |handle| {
            switch (builtin.os.tag) {
                .windows => {
                    const kernel32 = std.os.windows.kernel32;
                    _ = kernel32.FreeLibrary(@ptrCast(handle));
                },
                else => {
                    const c_lib = @cImport(@cInclude("dlfcn.h"));
                    _ = c_lib.dlclose(handle);
                },
            }
        }

        self.env_vars.deinit();
        self.allocator.free(self.core_path);
        self.allocator.free(self.system_path);
        g_instance = null;
    }

    pub fn loadCore(self: *LibRetroCore) !void {
        // Load dynamic library cross-platform
        const handle = switch (builtin.os.tag) {
            .windows => blk: {
                const wide_path = try std.unicode.utf8ToUtf16LeAllocZ(self.allocator, self.core_path);
                defer self.allocator.free(wide_path);

                const kernel32 = std.os.windows.kernel32;
                const h = kernel32.LoadLibraryW(wide_path.ptr);
                if (h == null) return LibRetroError.CoreLoadFailed;
                break :blk @as(*anyopaque, @ptrCast(h));
            },
            else => blk: {
                const c_lib = @cImport(@cInclude("dlfcn.h"));
                const core_path_z = try self.allocator.dupeZ(u8, self.core_path);
                defer self.allocator.free(core_path_z);

                const h = c_lib.dlopen(core_path_z.ptr, c_lib.RTLD_LOCAL | c_lib.RTLD_LAZY);
                if (h == null) {
                    print("Failed to load core: {s}\n", .{c_lib.dlerror()});
                    return LibRetroError.CoreLoadFailed;
                }
                break :blk h;
            },
        };

        self.core_handle = handle;

        // Load all required functions
        self.retro_init = self.getSymbol(*const fn () callconv(.C) void, "retro_init");
        self.retro_deinit = self.getSymbol(*const fn () callconv(.C) void, "retro_deinit");
        self.retro_run = self.getSymbol(*const fn () callconv(.C) void, "retro_run");
        self.retro_load_game = self.getSymbol(*const fn (*const c.retro_game_info) callconv(.C) bool, "retro_load_game");
        self.retro_unload_game = self.getSymbol(*const fn () callconv(.C) void, "retro_unload_game");
        self.retro_set_video_refresh = self.getSymbol(*const fn (*const fn (?*const anyopaque, c_uint, c_uint, usize) callconv(.C) void) callconv(.C) void, "retro_set_video_refresh");
        self.retro_set_environment = self.getSymbol(*const fn (*const fn (c_uint, ?*anyopaque) callconv(.C) bool) callconv(.C) void, "retro_set_environment");
        self.retro_set_input_poll = self.getSymbol(*const fn (*const fn () callconv(.C) void) callconv(.C) void, "retro_set_input_poll");
        self.retro_set_input_state = self.getSymbol(*const fn (*const fn (c_uint, c_uint, c_uint, c_uint) callconv(.C) i16) callconv(.C) void, "retro_set_input_state");
        self.retro_set_audio_sample = self.getSymbol(*const fn (*const fn (i16, i16) callconv(.C) void) callconv(.C) void, "retro_set_audio_sample");
        self.retro_set_audio_sample_batch = self.getSymbol(*const fn (*const fn ([*c]const i16, usize) callconv(.C) usize) callconv(.C) void, "retro_set_audio_sample_batch");
        self.retro_get_system_info = self.getSymbol(*const fn (*c.retro_system_info) callconv(.C) void, "retro_get_system_info");
        self.retro_get_system_av_info = self.getSymbol(*const fn (*c.retro_system_av_info) callconv(.C) void, "retro_get_system_av_info");
        self.retro_get_memory_data = self.getSymbol(*const fn (c_uint) callconv(.C) ?*anyopaque, "retro_get_memory_data");
        self.retro_get_memory_size = self.getSymbol(*const fn (c_uint) callconv(.C) usize, "retro_get_memory_size");

        // Verify required functions are loaded
        if (self.retro_init == null or self.retro_deinit == null or self.retro_run == null or
            self.retro_load_game == null or self.retro_unload_game == null or
            self.retro_set_video_refresh == null or self.retro_set_environment == null or
            self.retro_set_input_poll == null or self.retro_set_input_state == null or
            (self.retro_set_audio_sample == null and self.retro_set_audio_sample_batch == null) or
            self.retro_get_system_info == null or self.retro_get_system_av_info == null)
        {
            return LibRetroError.FunctionLoadFailed;
        }

        // Set up callbacks
        self.retro_set_environment.?(environmentCallback);
        self.retro_get_system_info.?(&g_system_info);

        if (g_system_info.library_name) |name| {
            print("Loaded core: {s}\n", .{name});
        }

        self.retro_set_video_refresh.?(videoRefreshCallback);
        self.retro_set_input_poll.?(inputPollCallback);
        self.retro_set_input_state.?(inputStateCallback);

        if (self.retro_set_audio_sample) |set_audio| set_audio(audioSampleCallback);
        if (self.retro_set_audio_sample_batch) |set_batch| set_batch(audioSampleBatchCallback);

        self.retro_init.?();
    }

    fn getSymbol(self: *LibRetroCore, comptime T: type, name: []const u8) ?T {
        if (self.core_handle == null) return null;

        return switch (builtin.os.tag) {
            .windows => blk: {
                const kernel32 = std.os.windows.kernel32;
                const proc = kernel32.GetProcAddress(@ptrCast(self.core_handle.?), name.ptr);
                break :blk if (proc) |p| @as(T, @ptrCast(p)) else null;
            },
            else => blk: {
                const c_lib = @cImport(@cInclude("dlfcn.h"));
                const name_z = self.allocator.dupeZ(u8, name) catch return null;
                defer self.allocator.free(name_z);

                const sym = c_lib.dlsym(self.core_handle.?, name_z.ptr);
                break :blk if (sym) |s| @as(T, @ptrCast(s)) else null;
            },
        };
    }

    pub fn loadROM(self: *LibRetroCore, rom_path: []const u8) !void {
        const file = std.fs.cwd().openFile(rom_path, .{}) catch return LibRetroError.RomLoadFailed;
        defer file.close();

        const file_size = try file.getEndPos();
        const buffer = try self.allocator.alloc(u8, file_size);
        defer self.allocator.free(buffer);

        _ = try file.readAll(buffer);

        const rom_path_z = try self.allocator.dupeZ(u8, rom_path);
        defer self.allocator.free(rom_path_z);

        const game_info = c.retro_game_info{
            .path = rom_path_z.ptr,
            .data = buffer.ptr,
            .size = buffer.len,
            .meta = null,
        };

        if (!self.retro_load_game.?(&game_info)) {
            print("Failed to load ROM: {s}\n", .{rom_path});
            return LibRetroError.RomLoadFailed;
        }

        self.retro_get_system_av_info.?(&g_av_info);
        print("Game loaded. Resolution: {d}x{d} @ {d:.1} fps\n", .{
            g_av_info.geometry.base_width,
            g_av_info.geometry.base_height,
            g_av_info.timing.fps,
        });
    }

    pub fn runCore(self: *LibRetroCore) !void {
        const fps = if (g_av_info.timing.fps > 0) g_av_info.timing.fps else 60.0;
        const frame_time_ns: u64 = @intFromFloat(1_000_000_000.0 / fps);

        print("Starting main loop at {d:.1} fps (frame time: {d}ns)\n", .{ fps, frame_time_ns });

        while (true) {
            const start = std.time.nanoTimestamp();
            self.retro_run.?();
            const end = std.time.nanoTimestamp();

            const elapsed: u64 = @intCast(end - start);
            if (elapsed < frame_time_ns) {
                std.time.sleep(frame_time_ns - elapsed);
            }
        }
    }

    pub fn setVideoFrameCallback(self: *LibRetroCore, callback: VideoFrameCallback) void {
        self.video_frame_callback = callback;
    }

    pub fn setAudioCallback(self: *LibRetroCore, callback: AudioCallback) void {
        self.audio_callback = callback;
    }

    pub fn setInputCallback(self: *LibRetroCore, callback: InputCallback) void {
        self.input_callback = callback;
    }

    pub fn loadSaveFile(self: *LibRetroCore, save_path: []const u8) !bool {
        self.save_mutex.lock();
        defer self.save_mutex.unlock();

        const file = std.fs.cwd().openFile(save_path, .{}) catch {
            print("No save file found at {s}\n", .{save_path});
            return false;
        };
        defer file.close();

        const save_data = self.retro_get_memory_data.?(c.RETRO_MEMORY_SAVE_RAM);
        const save_size = self.retro_get_memory_size.?(c.RETRO_MEMORY_SAVE_RAM);

        if (save_data == null or save_size == 0) {
            print("Core does not support save RAM\n", .{});
            return false;
        }

        const bytes_read = try file.readAll(@as([*]u8, @ptrCast(save_data))[0..save_size]);
        const success = bytes_read == save_size;

        if (success) {
            print("Successfully loaded save file: {s} ({d} bytes)\n", .{ save_path, save_size });
        } else {
            print("Failed to load save file or size mismatch\n", .{});
        }

        return success;
    }

    pub fn saveSaveFile(self: *LibRetroCore, save_path: []const u8) !bool {
        self.save_mutex.lock();
        defer self.save_mutex.unlock();

        const save_data = self.retro_get_memory_data.?(c.RETRO_MEMORY_SAVE_RAM);
        const save_size = self.retro_get_memory_size.?(c.RETRO_MEMORY_SAVE_RAM);

        if (save_data == null or save_size == 0) {
            print("Core does not support save RAM\n", .{});
            return false;
        }

        // Create directory if it doesn't exist
        if (std.fs.path.dirname(save_path)) |dir| {
            std.fs.cwd().makePath(dir) catch {};
        }

        const file = std.fs.cwd().createFile(save_path, .{}) catch {
            print("Failed to open save file for writing: {s}\n", .{save_path});
            return false;
        };
        defer file.close();

        const data_slice = @as([*]const u8, @ptrCast(save_data))[0..save_size];
        file.writeAll(data_slice) catch {
            print("Failed to write save file\n", .{});
            return false;
        };

        print("Successfully saved game to: {s} ({d} bytes)\n", .{ save_path, save_size });
        return true;
    }

    fn logEnvironmentVariables(self: *LibRetroCore, vars: [*c]const c.retro_variable) void {
        if (builtin.mode != .Debug) return;

        print("Environment variables set by core:\n", .{});
        print("-----------------------------\n", .{});

        var i: usize = 0;
        while (true) {
            const var_ptr = vars + i;
            if (var_ptr.*.key == null or var_ptr.*.value == null) break;

            const key = std.mem.span(var_ptr.*.key);
            const value = std.mem.span(var_ptr.*.value);

            print("Key: {s}\n", .{key});
            print("Options: {s}\n", .{value});

            if (std.mem.indexOf(u8, value, ";")) |semicolon| {
                var start = semicolon + 1;
                while (start < value.len and (value[start] == ' ' or value[start] == '\t')) {
                    start += 1;
                }

                if (start < value.len) {
                    const end = std.mem.indexOf(u8, value[start..], "|") orelse (value.len - start);
                    const default_value = value[start .. start + end];

                    const key_owned = self.allocator.dupe(u8, key) catch continue;
                    const value_owned = self.allocator.dupe(u8, default_value) catch {
                        self.allocator.free(key_owned);
                        continue;
                    };

                    self.env_vars.variables.put(key_owned, value_owned) catch {
                        self.allocator.free(key_owned);
                        self.allocator.free(value_owned);
                        continue;
                    };

                    print("Default: {s}\n", .{default_value});
                }
            }
            print("-----------------------------\n", .{});
            i += 1;
        }
    }
};

// C callback functions
fn videoRefreshCallback(data: ?*const anyopaque, width: c_uint, height: c_uint, pitch: usize) callconv(.C) void {
    const instance = g_instance orelse return;
    const callback = instance.video_frame_callback orelse return;

    if (data) |d| {
        switch (instance.pixel_format) {
            .xrgb8888 => {
                const src = @as([*]const u32, @ptrCast(@alignCast(d)));
                const converted_data = instance.allocator.alloc(u16, width * height) catch return;
                defer instance.allocator.free(converted_data);

                for (0..height) |y| {
                    for (0..width) |x| {
                        const rgb8888 = src[y * pitch / 4 + x];
                        converted_data[y * width + x] = rgb8888ToRgb565(rgb8888);
                    }
                }

                const data_bytes = std.mem.sliceAsBytes(converted_data);
                callback(data_bytes, width, height, width * 2);
            },
            else => {
                const size = height * pitch;
                const data_slice = @as([*]const u8, @ptrCast(d))[0..size];
                callback(data_slice, width, height, pitch);
            },
        }
    }
}

fn environmentCallback(cmd: c_uint, data: ?*anyopaque) callconv(.C) bool {
    const instance = g_instance orelse return false;

    switch (cmd) {
        c.RETRO_ENVIRONMENT_GET_LOG_INTERFACE => {
            if (data) |d| {
                const cb = @as(*c.retro_log_callback, @ptrCast(@alignCast(d)));
                cb.log = logPrintf;
                return true;
            }
        },
        c.RETRO_ENVIRONMENT_GET_CAN_DUPE => {
            if (data) |d| {
                const can_dupe = @as(*bool, @ptrCast(@alignCast(d)));
                can_dupe.* = true;
                return true;
            }
        },
        c.RETRO_ENVIRONMENT_SET_PIXEL_FORMAT => {
            if (data) |d| {
                const fmt = @as(*c.retro_pixel_format, @ptrCast(@alignCast(d)));
                if (fmt.* > c.RETRO_PIXEL_FORMAT_RGB565) return false;
                instance.pixel_format = @enumFromInt(fmt.*);
                return true;
            }
        },
        c.RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY, c.RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY, c.RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY => {
            if (data) |d| {
                const path_ptr = @as(*[*c]const u8, @ptrCast(@alignCast(d)));
                path_ptr.* = instance.system_path.ptr;
                return true;
            }
        },
        c.RETRO_ENVIRONMENT_SET_VARIABLES => {
            if (data) |d| {
                if (builtin.mode == .Debug) {
                    const vars = @as([*c]const c.retro_variable, @ptrCast(@alignCast(d)));
                    instance.logEnvironmentVariables(vars);
                }
                return true;
            }
        },
        c.RETRO_ENVIRONMENT_GET_VARIABLE => {
            if (data) |d| {
                const var_ptr = @as(*c.retro_variable, @ptrCast(@alignCast(d)));
                if (var_ptr.key) |key| {
                    const key_slice = std.mem.span(key);
                    if (instance.env_vars.variables.get(key_slice)) |value| {
                        var_ptr.value = value.ptr;
                        if (builtin.mode == .Debug) {
                            print("Retrieving variable: {s} = {s}\n", .{ key_slice, value });
                        }
                        return true;
                    }
                    if (builtin.mode == .Debug) {
                        print("Variable not found: {s}\n", .{key_slice});
                    }
                    var_ptr.value = null;
                }
            }
            return false;
        },
        c.RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE => {
            if (data) |d| {
                const updated = @as(*bool, @ptrCast(@alignCast(d)));
                updated.* = instance.env_vars.updated;
                if (instance.env_vars.updated) {
                    if (builtin.mode == .Debug) {
                        print("Variables updated flag read and reset\n", .{});
                    }
                    instance.env_vars.updated = false;
                }
                return true;
            }
        },
        else => return false,
    }
    return false;
}

fn inputPollCallback() callconv(.C) void {
    // Nothing to do
}

fn inputStateCallback(port: c_uint, device: c_uint, index: c_uint, id: c_uint) callconv(.C) i16 {
    _ = device;
    _ = index;
    const instance = g_instance orelse return 0;
    const callback = instance.input_callback orelse return 0;
    return callback(port, id);
}

fn audioSampleCallback(left: i16, right: i16) callconv(.C) void {
    _ = left;
    _ = right;
    // Nothing to do for single samples
}

fn audioSampleBatchCallback(data: [*c]const i16, frames: usize) callconv(.C) usize {
    const instance = g_instance orelse return frames;
    const callback = instance.audio_callback orelse return frames;

    if (data) |d| {
        const audio_data = d[0 .. frames * 2]; // Stereo
        callback(audio_data);
    }
    return frames;
}
