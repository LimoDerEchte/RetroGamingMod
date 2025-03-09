package com.limo.emumod.bridge;

import com.limo.emumod.util.RequirementManager;
import com.limo.emumod.util.FileUtil;

import java.io.File;
import java.util.UUID;

public class NativeGenericConsole {
    private final long pointer;
    private NativeDisplay nativeDisplay;
    private NativeAudio nativeAudio;

    public NativeGenericConsole(int width, int height) {
        pointer = init(width, height);
    }

    public void load(File core, UUID file) {
        File rom = FileUtil.idToFile(file, "cart");
        File save = FileUtil.idToFile(file, "save");
        start(pointer, RequirementManager.core.getAbsolutePath(), core.getAbsolutePath(), rom.getAbsolutePath(), save.getAbsolutePath());
    }

    public void stop() {
        stop(pointer);
    }

    public NativeDisplay createDisplay() {
        if(nativeDisplay == null)
            nativeDisplay = new NativeDisplay(createDisplay(pointer));
        return nativeDisplay;
    }

    public NativeAudio createAudio() {
        if(nativeAudio == null)
            nativeAudio = new NativeAudio(createAudio(pointer));
        return nativeAudio;
    }

    public void updateInput(short input) {
        updateInput(pointer, input);
    }

    private native static long init(int width, int height);
    private native static void start(long pointer, String retroCore, String core, String rom, String save);
    private native static void stop(long pointer);
    private native static void updateInput(long pointer, short input);
    private native static long createDisplay(long pointer);
    private native static long createAudio(long pointer);
}
