package com.limo.emumod.bridge;

import com.limo.emumod.EmuMod;

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
        EmuMod.LOGGER.info(buf.length);
        return buf;
    }

    private static native boolean hasChanged(long pointer);
    private native void update(long pointer);
}
