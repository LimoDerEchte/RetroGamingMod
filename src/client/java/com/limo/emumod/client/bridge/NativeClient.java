package com.limo.emumod.client.bridge;

import com.limo.emumod.bridge.NativeUtil;

import java.util.UUID;

public class NativeClient {
    private final long handle;

    public NativeClient(String ip, int port, String token) {
        handle = connect(ip, port, token);
    }

    public void disconnect() {
        disconnect(handle);
    }

    public void poll() {
        poll(handle);
    }

    public boolean isAuthenticated() {
        return isAuthenticated(handle);
    }

    public void registerScreen(UUID uuid, int width, int height) {
        registerScreen(handle, NativeUtil.nativeUUID(uuid), width, height);
    }

    public void unregisterScreen(UUID uuid) {
        unregisterScreen(handle, NativeUtil.nativeUUID(uuid));
    }

    private static native long connect(String ip, int port, String token);
    private static native void disconnect(long ptr);
    private static native boolean isAuthenticated(long ptr);
    private static native void registerScreen(long ptr, long uuid, int width, int height);
    private static native void unregisterScreen(long ptr, long uuid);
    private static native void poll(long ptr);
}
