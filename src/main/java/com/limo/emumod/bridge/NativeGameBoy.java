package com.limo.emumod.bridge;

import com.limo.emumod.util.RequirementManager;
import com.limo.emumod.util.FileUtil;

import java.io.File;
import java.util.UUID;

public class NativeGameBoy {
    private final long pointer;
    private NativeDisplay nativeDisplay;
    private NativeAudio nativeAudio;

    public NativeGameBoy(boolean isGBA) {
        pointer = init(isGBA);
    }

    public void load(UUID file) {
        File rom = FileUtil.idToFile(file, "cart");
        File save = FileUtil.idToFile(file, "save");
        start(pointer, RequirementManager.core.getAbsolutePath(), RequirementManager.mGBA.getAbsolutePath(), rom.getAbsolutePath(), save.getAbsolutePath());
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

    private native static long init(boolean isGBA);
    private native static void start(long pointer, String retroCore, String core, String rom, String save);
    private native static void stop(long pointer);
    private native static void updateInput(long pointer, short input);
    private native static long createDisplay(long pointer);
    private native static long createAudio(long pointer);
}
