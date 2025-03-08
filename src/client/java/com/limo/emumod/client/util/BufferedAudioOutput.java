package com.limo.emumod.client.util;

import com.limo.emumod.EmuMod;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BufferedAudioOutput {
    private long device;
    private long context;
    private int[] buffers;
    private int source;
    private static final int BUFFER_COUNT = 5;
    private static final int SAMPLE_RATE = 44100;

    public BufferedAudioOutput() {
        initialize();
    }

    private void initialize() {
        device = ALC11.alcOpenDevice((ByteBuffer) null);
        if (device == 0) {
            throw new RuntimeException("Failed to open the default OpenAL device");
        }

        IntBuffer contextAttribs = MemoryUtil.memAllocInt(16);
        contextAttribs.put(ALC11.ALC_FREQUENCY);
        contextAttribs.put(SAMPLE_RATE);
        contextAttribs.put(ALC11.ALC_REFRESH);
        contextAttribs.put(60);
        contextAttribs.put(ALC11.ALC_SYNC);
        contextAttribs.put(ALC11.ALC_FALSE);
        contextAttribs.flip();

        context = ALC11.alcCreateContext(device, contextAttribs);
        MemoryUtil.memFree(contextAttribs);
        if (context == 0) {
            ALC11.alcCloseDevice(device);
            throw new RuntimeException("Failed to create OpenAL context");
        }

        ALC11.alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));

        buffers = new int[BUFFER_COUNT];
        AL11.alGenBuffers(buffers);

        source = AL11.alGenSources();
        checkAlError();
        AL11.alSourcei(source, AL11.AL_LOOPING, AL11.AL_FALSE);
        checkAlError();

        int error = AL11.alGetError();
        if (error != AL11.AL_NO_ERROR) {
            cleanup();
            throw new RuntimeException("OpenAL error: " + error);
        }

        for (int i = 0; i < BUFFER_COUNT; i++) {
            ShortBuffer silentBuffer = MemoryUtil.memAllocShort(0);
            silentBuffer.flip();

            AL11.alBufferData(buffers[i], AL11.AL_FORMAT_STEREO16, silentBuffer, SAMPLE_RATE);
            AL11.alSourceQueueBuffers(source, buffers[i]);
            MemoryUtil.memFree(silentBuffer);
        }

        AL11.alSourcePlay(source);
        checkAlError();
    }

    public void playAudio(short[] audioData) {
        int processedBuffers = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED);
        if (processedBuffers <= 0) {
            checkAlError();
            return;
        }

        IntBuffer unqueuedBuffersInt = MemoryUtil.memAllocInt(1);
        AL11.alSourceUnqueueBuffers(source, unqueuedBuffersInt);
        int bufferID = unqueuedBuffersInt.get(0);
        MemoryUtil.memFree(unqueuedBuffersInt);
        checkAlError();

        ShortBuffer dataBuffer = MemoryUtil.memAllocShort(audioData.length);
        dataBuffer.put(audioData);
        dataBuffer.flip();

        AL11.alBufferData(bufferID, AL11.AL_FORMAT_STEREO16, dataBuffer, SAMPLE_RATE);
        MemoryUtil.memFree(dataBuffer);
        checkAlError();

        AL11.alSourceQueueBuffers(source, bufferID);
        checkAlError();

        int sourceState = AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE);
        if (sourceState != AL11.AL_PLAYING) {
            AL11.alSourcePlay(source);
        }
        checkAlError();
    }

    public void cleanup() {
        if (source != 0) {
            AL11.alSourceStop(source);
            AL11.alDeleteSources(source);
        }
        if (buffers != null) {
            AL11.alDeleteBuffers(buffers);
        }
        if (context != 0) {
            ALC11.alcDestroyContext(context);
        }
        if (device != 0) {
            ALC11.alcCloseDevice(device);
        }
    }

    public static void checkAlError() {
        int error = AL11.alGetError();
        if (error == AL11.AL_NO_ERROR) {
            return;
        }
        StackTraceElement stack = Thread.currentThread().getStackTrace()[2];
        EmuMod.LOGGER.error("AL error: {}.{}[{}] {}", stack.getClassName(), stack.getMethodName(), stack.getLineNumber(), switch (error) {
            case AL11.AL_INVALID_NAME -> "Invalid name";
            case AL11.AL_INVALID_ENUM -> "Invalid enum";
            case AL11.AL_INVALID_VALUE -> "Invalid value";
            case AL11.AL_INVALID_OPERATION -> "Invalid operation";
            case AL11.AL_OUT_OF_MEMORY -> "Out of memory";
            default -> "Unknown error";
        });
    }
}
