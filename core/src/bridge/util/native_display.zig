
const std = @import("std");
const jni = @import("jni");
const decoder = @import("../codec/video_decoder.zig");

// JNI
pub fn bufSize(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong) callconv(.C) jni.jint {
    const display: *NativeDisplay = @ptrFromInt(@as(usize, @intCast(ptr)));
    display.mutex.lock();
    defer display.mutex.unlock();
    return display.width * display.height;
}

pub fn hasChanged(_: *jni.cEnv, _: jni.jclass, ptr: jni.jlong) callconv(.C) jni.jboolean {
    const display: *NativeDisplay = @ptrFromInt(@as(usize, @intCast(ptr)));
    display.mutex.lock();
    defer display.mutex.unlock();
    return jni.boolToJboolean(display.changed);
}

pub fn update(env: *jni.cEnv, obj: jni.jobject, ptr: jni.jlong) callconv(.C) void {
    const display: *NativeDisplay = @ptrFromInt(@as(usize, @intCast(ptr)));
    display.mutex.lock();
    defer display.mutex.unlock();
    if(!display.changed)
        return;
    display.changed = false;
    const class: jni.jclass = env.*.*.GetObjectClass.?(env, obj);
    const field: jni.jfieldID = env.*.*.GetFieldID.?(env, class, "buf", "[I");
    const data: jni.jintArray = env.*.*.GetObjectField.?(env, obj, field);
    env.*.*.SetIntArrayRegion.?(env, data, 0, @intCast(display.buf.items.len), @ptrCast(&display.buf.items[0]));
}

// Source
pub const NativeDisplay = struct {
    decoder: ?decoder.VideoDecoderInt16 = null,
    mutex: std.Thread.Mutex = .{},
    changed: bool = false,
    buf: std.ArrayList(u32),
    width: i32,
    height: i32,

    pub fn init(width: i32, height: i32) NativeDisplay {
        return .{
            .buf = std.ArrayList(u32).init(std.heap.page_allocator),
            .width = width,
            .height = height,
        };
    }

    pub fn receive(self: *NativeDisplay, data: *std.ArrayList(u8)) !void {
        self.mutex.lock();
        if(self.decoder == null) {
            self.decoder = decoder.VideoDecoderInt16.init(self.width, self.height);
        }
        const frame = try self.decoder.?.decodeFrameRGB565(data);
        try self.buf.replaceRange(0, frame.items.len, frame.items);
        self.changed = true;
    }
};
