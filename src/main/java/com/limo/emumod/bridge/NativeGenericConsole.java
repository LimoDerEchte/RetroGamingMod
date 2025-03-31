package com.limo.emumod.bridge;

import com.limo.emumod.EmuMod;
import com.limo.emumod.util.RequirementManager;
import com.limo.emumod.util.FileUtil;

import java.io.File;
import java.util.UUID;

public class NativeGenericConsole {
    private final long pointer;
    private final UUID file;
    private final String fileType;

    public NativeGenericConsole(int width, int height, int sampleRate, UUID file, String fileType) {
        this.file = file;
        this.fileType = fileType;
        pointer = init(NativeUtil.nativeUUID(file), width, height, sampleRate);
    }

    public void load(File core) {
        File rom = FileUtil.idToFile(file, fileType);
        File save = FileUtil.idToFile(file, "save");
        start(pointer, RequirementManager.core.getAbsolutePath(), core.getAbsolutePath(), rom.getAbsolutePath(), save.getAbsolutePath());
    }

    public void stop() {
        stop(pointer);
    }

    public int getWidth() {
        return getWidth(pointer);
    }

    public int getHeight() {
        return getHeight(pointer);
    }

    public int getSampleRate() {
        return getSampleRate(pointer);
    }

    private native static long init(long uuid, int width, int height, int sampleRate);
    private native static void start(long pointer, String retroCore, String core, String rom, String save);
    private native static void stop(long pointer);

    private native static int getWidth(long pointer);
    private native static int getHeight(long pointer);
    private native static int getSampleRate(long pointer);
}
