package com.limo.emumod.client.bridge;

import com.limo.emumod.bridge.NativeUtil;
import net.minecraft.client.texture.NativeImage;

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

    public NativeImage registerScreen(UUID uuid, int width, int height, int sampleRate) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, width, height, true);
        registerScreen(handle, NativeUtil.nativeUUID(uuid), width, height, img.imageId(), sampleRate);
        return img;
    }

    public void unregisterScreen(UUID uuid) {
        unregisterScreen(handle, NativeUtil.nativeUUID(uuid));
    }

    public boolean screenChanged(UUID uuid) {
        return screenChanged(handle, NativeUtil.nativeUUID(uuid));
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
    private static native void registerScreen(long ptr, long uuid, int width, int height, long data, int sampleRate);
    private static native void unregisterScreen(long ptr, long uuid);
    private static native boolean screenChanged(long ptr, long uuid);
    private static native void sendControlUpdate(long ptr, long uuid, int port, short controls);
    private static native void updateAudioDistance(long ptr, long uuid, double distance);
}
