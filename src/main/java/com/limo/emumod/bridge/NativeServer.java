package com.limo.emumod.bridge;

public class NativeServer {
    public static native boolean init(int maxUsers, String bind, String[] addresses);
    public static native boolean deinit();
    public static native byte[] generateToken();
}
