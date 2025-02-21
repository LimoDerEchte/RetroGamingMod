package com.limo.emumod.bridge;

public class NativeDisplay {
    private final long pointer;
    private final int[] buf;

    public NativeDisplay(long pointer) {
        this.pointer = pointer;
        this.buf = new int[bufSize(pointer)];
    }

    public int[] getBuf() {
        update(pointer);
        return buf;
    }

    private static native int bufSize(long pointer);
    private native void update(long pointer);
}
