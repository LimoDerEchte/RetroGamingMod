package com.limo.emumod.bridge;

import com.limo.emumod.util.FileUtil;

import java.io.File;
import java.util.UUID;

public class NativeGameBoy {
    private final long pointer;
    private NativeDisplay nativeDisplay;

    public NativeGameBoy() {
        pointer = init();
    }

    public void load(UUID file) {
        File rom = FileUtil.idToFile(file, "cart");
        File save = FileUtil.idToFile(file, "save");
        start(pointer, rom.getAbsolutePath(), save.getAbsolutePath());
    }

    public void stop() {
        stop(pointer);
    }

    public NativeDisplay createDisplay() {
        if(nativeDisplay == null)
            nativeDisplay = new NativeDisplay(createDisplay(pointer));
        return nativeDisplay;
    }

    private native static long init();
    private native static void start(long pointer, String rom, String save);
    private native static void stop(long pointer);
    private native static long createDisplay(long pointer);
}
