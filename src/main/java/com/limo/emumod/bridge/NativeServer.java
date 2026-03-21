package com.limo.emumod.bridge;

public class NativeServer {

    public static byte[] createToken() {
        byte[] arr = generateToken();
        if (arr.length == 0)
            throw new RuntimeException("Token size should never be zero!");
        return arr;
    }

    public static native boolean init(int maxUsers, String bind, String[] addresses);
    public static native boolean deinit();
    public static native byte[] generateToken();
}
