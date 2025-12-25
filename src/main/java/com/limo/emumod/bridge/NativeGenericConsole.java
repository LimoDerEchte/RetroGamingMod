package com.limo.emumod.bridge;

import com.limo.emumod.util.RequirementManager;
import com.limo.emumod.util.FileUtil;

import java.io.File;
import java.util.UUID;

public class NativeGenericConsole {
    private final long pointer;
    private final UUID file;
    private final String fileType;
    private final int codec;

    public NativeGenericConsole(int width, int height, int sampleRate, int codec, UUID file, UUID consoleId, String fileType) {
        this.file = file;
        this.fileType = fileType;
        this.codec = codec;
        pointer = init(NativeUtil.nativeUUID(file), NativeUtil.nativeUUID(consoleId), width, height, sampleRate, codec);
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

    public int getCodec() {
        return codec;
    }

    private native static long init(long uuid, long consoleId, int width, int height, int sampleRate, int codec);
    private native static void start(long pointer, String retroCore, String core, String rom, String save);
    private native static void stop(long pointer);

    private native static int getWidth(long pointer);
    private native static int getHeight(long pointer);
    private native static int getSampleRate(long pointer);
}
