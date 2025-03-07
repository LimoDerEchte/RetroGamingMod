package com.limo.emumod.bridge;

public class NativeAudio {
    private final long pointer;
    private final short[] buf;

    public NativeAudio(long pointer) {
        this.pointer = pointer;
        this.buf = new short[0];
    }

    public boolean hasChanged() {
        return hasChanged(pointer);
    }

    public short[] getBuf() {
        update(pointer);
        return buf;
    }

    private static native boolean hasChanged(long pointer);
    private native void update(long pointer);
}
