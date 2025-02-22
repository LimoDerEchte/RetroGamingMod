package com.limo.emumod.bridge;

import com.limo.emumod.util.FileUtil;

import java.io.File;
import java.util.UUID;

public class NativeGameBoy {
    private final long pointer;

    public NativeGameBoy() {
        pointer = init();
    }

    public void load(UUID file) {
        File rom = FileUtil.idToFile(file, "cart");
        File save = FileUtil.idToFile(file, "save");
        loadROM(pointer, rom.getAbsolutePath());
        loadSave(pointer, save.getAbsolutePath());
        start(pointer);
    }

    public void stop() {
        stop(pointer);
    }

    private native static long init();
    private native static void loadROM(long pointer, String path);
    private native static void loadSave(long pointer, String path);
    private native static void start(long pointer);
    private native static void stop(long pointer);
}
