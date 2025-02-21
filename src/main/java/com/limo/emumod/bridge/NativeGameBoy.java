package com.limo.emumod.bridge;

import com.sun.jna.Pointer;

import java.io.File;

public class NativeGameBoy {
    private final Pointer pointer;

    public NativeGameBoy() {
        pointer = init();
    }

    public void load(File file) {
        load(pointer, file.getAbsolutePath());
    }

    private native static Pointer init();
    private native static void load(Pointer pointer, String path);
}
