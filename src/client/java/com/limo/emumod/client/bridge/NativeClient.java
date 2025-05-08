package com.limo.emumod.client.bridge;

import com.limo.emumod.bridge.NativeDisplay;
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

    public boolean isAuthenticated() {
        return isAuthenticated(handle);
    }

    public NativeDisplay registerScreen(UUID uuid, int width, int height, int sampleRate) {
        long ptr = registerScreen(handle, NativeUtil.nativeUUID(uuid), width, height, sampleRate);
        return new NativeDisplay(ptr);
    }

    public void unregisterScreen(UUID uuid) {
        unregisterScreen(handle, NativeUtil.nativeUUID(uuid));
    }

    public void updateControls(UUID uuid, int port, short controls) {
        sendControlUpdate(handle, NativeUtil.nativeUUID(uuid), port, controls);
    }

    public void updateAudioDistance(UUID uuid, double audioDistance) {
        updateAudioDistance(handle, NativeUtil.nativeUUID(uuid), audioDistance);
    }

    private static native long connect(String ip, int port, String token);
    private static native void disconnect(long ptr);
    private static native boolean isAuthenticated(long ptr);
    private static native long registerScreen(long ptr, long uuid, int width, int height, int sampleRate);
    private static native void unregisterScreen(long ptr, long uuid);
    private static native void sendControlUpdate(long ptr, long uuid, int port, short controls);
    private static native void updateAudioDistance(long ptr, long uuid, double distance);
}
