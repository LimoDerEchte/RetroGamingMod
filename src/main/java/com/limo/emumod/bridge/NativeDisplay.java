package com.limo.emumod.bridge;

import com.sun.jna.Pointer;

public class NativeDisplay {
    private final Pointer display;
    private final int[] buf;

    public NativeDisplay(Pointer display, int bufSize) {
        this.display = display;
        this.buf = new int[bufSize];
    }

    public int[] retrieveBuf() {
        update(display);
        return buf;
    }

    private native void update(Pointer display);
}
