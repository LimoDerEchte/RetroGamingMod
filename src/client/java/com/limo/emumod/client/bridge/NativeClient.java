package com.limo.emumod.client.bridge;

import com.limo.emumod.util.VideoCodec;
import net.minecraft.client.texture.NativeImage;

public class NativeClient {
    public static boolean isInitialized = false;

    public static NativeImage registerScreen(int id, int width, int height, int sampleRate, VideoCodec codec) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, width, height, true);
        registerId(id, width, height, codec.ordinal(), img.imageId());
        return img;
    }

    public static void unregisterScreen(int id) {
        unregisterId(id);
    }

    public static void updateControls(int id, short port, short controls) {
        sendControls(id, port, controls);
    }

    public static native boolean init(String ip, short port, byte[] token);
    public static native boolean deinit();

    public static native boolean isConnected();
    public static native boolean screenChanged(int id);
    // TODO: public static native float[] lastAudioData(int id); --- ONLY IMPLEMENT IF JAVA NATIVE AUDIO PLAYBACK

    private static native void registerId(int id, int width, int height, int codec, long data);
    private static native void unregisterId(int id);
    private static native void sendControls(int id, short port, short controls);
}
