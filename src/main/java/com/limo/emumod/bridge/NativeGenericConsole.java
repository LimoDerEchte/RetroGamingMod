package com.limo.emumod.bridge;

import com.limo.emumod.util.AudioCodec;
import com.limo.emumod.util.RequirementManager;
import com.limo.emumod.util.FileUtil;
import com.limo.emumod.util.VideoCodec;

import java.io.File;
import java.util.UUID;

public class NativeGenericConsole {
    private final int id;

    private final int width, height;
    private final VideoCodec videoCodec;
    private final AudioCodec audioCodec;

    private final UUID file;
    private final String fileType;
    private final int sampleRate;

    public NativeGenericConsole(int width, int height, VideoCodec videoCodec, AudioCodec audioCodec, UUID file, String fileType, int sampleRate) {
        this.videoCodec = videoCodec;
        this.audioCodec = audioCodec;
        this.width = width;
        this.height = height;
        this.sampleRate = sampleRate;

        this.file = file;
        this.fileType = fileType;
        this.id = register(width, height, videoCodec.ordinal(), audioCodec.ordinal());
    }

    public void load(File core) {
        File rom = FileUtil.idToFile(file, fileType);
        File save = FileUtil.idToFile(file, "save");
        start(id, RequirementManager.core.getAbsolutePath(), core.getAbsolutePath(), rom.getAbsolutePath(), save.getAbsolutePath());
    }

    public void stop() {
        unregister(id);
    }

    public int getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public VideoCodec getVideoCodec() {
        return videoCodec;
    }

    public AudioCodec getAudioCodec() {
        return audioCodec;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    private static native int register(int width, int height, int videoCodec, int audioCodec);
    private static native void unregister(int id);
    private static native void start(int id, String retroCore, String core, String rom, String save);
}
