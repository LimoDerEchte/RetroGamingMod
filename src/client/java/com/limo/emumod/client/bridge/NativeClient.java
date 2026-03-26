package com.limo.emumod.client.bridge;

import com.limo.emumod.util.AudioCodec;
import com.limo.emumod.util.VideoCodec;
import com.mojang.blaze3d.platform.NativeImage;

public class NativeClient {

    public static NativeImage registerScreen(int id, int width, int height, VideoCodec videoCodec, AudioCodec audioCodec) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, width, height, true);
        registerId(id, width, height, videoCodec.ordinal(), img.getPointer(), audioCodec.ordinal());
        return img;
    }

    public static void unregisterScreen(int id) {
        unregisterId(id);
    }

    public static void updateControls(int id, short port, short controls) {
        sendControls(id, port, controls);
    }

    public static native boolean init(byte[] token);
    public static native boolean deinit();

    public static native boolean isConnected();
    public static native boolean screenChanged(int id);
    // TODO: public static native float[] lastAudioData(int id); --- ONLY IMPLEMENT IF JAVA NATIVE AUDIO PLAYBACK

    private static native void registerId(int id, int width, int height, int videoCodec, long data, int audioCodec);
    private static native void unregisterId(int id);
    private static native void sendControls(int id, short port, short controls);
}
