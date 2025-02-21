package com.limo.emumod.bridge;

import java.io.File;

public class NativeGameBoy {
    private final long pointer;

    public NativeGameBoy() {
        pointer = init();
    }

    public void load(File file) {
        load(pointer, file.getAbsolutePath());
    }

    public void stop() {
        stop(pointer);
    }

    private native static long init();
    private native static void load(long pointer, String path);
    private native static void stop(long pointer);
}
