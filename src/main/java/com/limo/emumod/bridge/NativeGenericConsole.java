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
    //private NativeDisplay nativeDisplay;
    //private NativeAudio nativeAudio;

    public NativeGenericConsole(int width, int height, UUID file, String fileType) {
        this.file = file;
        this.fileType = fileType;
        pointer = init(NativeUtil.nativeUUID(file), width, height);
    }

    public void load(File core) {
        File rom = FileUtil.idToFile(file, fileType);
        File save = FileUtil.idToFile(file, "save");
        start(pointer, RequirementManager.core.getAbsolutePath(), core.getAbsolutePath(), rom.getAbsolutePath(), save.getAbsolutePath());
    }

    public void stop() {
        stop(pointer);
    }

    //public NativeDisplay createDisplay() {
    //    if(nativeDisplay == null)
    //        nativeDisplay = new NativeDisplay(createDisplay(pointer));
    //    return nativeDisplay;
    //}

    //public NativeAudio createAudio() {
    //    if(nativeAudio == null)
    //        nativeAudio = new NativeAudio(createAudio(pointer));
    //    return nativeAudio;
    //}

    public void updateInput(int port, short input) {
        if(port < 0 || port > 4) {
            EmuMod.LOGGER.warn("Invalid controls port number: {}", port);
            return;
        }
        updateInput(pointer, port, input);
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

    private native static long init(long uuid, int width, int height);
    private native static void start(long pointer, String retroCore, String core, String rom, String save);
    private native static void stop(long pointer);

    private native static void updateInput(long pointer, int port, short input);
    //private native static long createDisplay(long pointer);
    //private native static long createAudio(long pointer);

    private native static int getWidth(long pointer);
    private native static int getHeight(long pointer);
    private native static int getSampleRate(long pointer);
}
