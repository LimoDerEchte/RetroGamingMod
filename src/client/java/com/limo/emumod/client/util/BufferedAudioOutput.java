package com.limo.emumod.client.util;

import com.limo.emumod.EmuMod;
import net.minecraft.entity.TrackedPosition;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.limo.emumod.client.EmuModClient.mc;

public class BufferedAudioOutput {
    private long device;
    private long context;
    private int[] buffers;
    private int[] sources;
    private static final int BUFFER_COUNT = 5;

    private final int sampleRate;
    private final List<TrackedPosition> trackedPositions = new ArrayList<>();

    public BufferedAudioOutput(int sampleRate, TrackedPosition initialPosition) {
        this.sampleRate = sampleRate;
        trackedPositions.add(initialPosition);
        initialize();
    }

    private void initialize() {
        device = ALC11.alcOpenDevice((ByteBuffer) null);
        if(device == 0) {
            throw new RuntimeException("Failed to open the default OpenAL device");
        }

        IntBuffer contextAttribs = MemoryUtil.memAllocInt(16);
        contextAttribs.put(ALC11.ALC_FREQUENCY);
        contextAttribs.put(sampleRate);
        contextAttribs.put(ALC11.ALC_REFRESH);
        contextAttribs.put(60);
        contextAttribs.put(ALC11.ALC_SYNC);
        contextAttribs.put(ALC11.ALC_FALSE);
        contextAttribs.flip();

        context = ALC11.alcCreateContext(device, contextAttribs);
        MemoryUtil.memFree(contextAttribs);
        if(context == 0) {
            ALC11.alcCloseDevice(device);
            throw new RuntimeException("Failed to create OpenAL context");
        }
        ALC11.alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));
        AL11.alDistanceModel(AL11.AL_INVERSE_DISTANCE_CLAMPED);
        checkAlError();

        genSources();
        genBuffers();
    }

    private void genSources() {
        sources = new int[trackedPositions.size()];

        AL11.alGenSources(sources);
        for(int i = 0; i < sources.length; i++) {
            Vec3d pos = trackedPositions.get(i).getPos();
            AL11.alSourcei(sources[i], AL11.AL_LOOPING, AL11.AL_FALSE);
            AL11.alSourcef(sources[i], AL11.AL_GAIN, 1.0f);
            AL11.alSource3f(sources[i], AL11.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
            AL11.alSourcef(sources[i], AL11.AL_ROLLOFF_FACTOR, 1.0f);
            AL11.alSourcef(sources[i], AL11.AL_REFERENCE_DISTANCE, 1.0f);
            AL11.alSourcef(sources[i], AL11.AL_MAX_DISTANCE, 16.0f);
        }

        int error = AL11.alGetError();
        if(error != AL11.AL_NO_ERROR) {
            cleanup();
            throw new RuntimeException("OpenAL error: " + error);
        }
    }

    private void genBuffers() {
        for(int source : sources) {
            AL11.alSourceStop(source);
        }
        if(buffers != null) {
            AL11.alDeleteBuffers(buffers);
        }
        buffers = new int[BUFFER_COUNT * trackedPositions.size()];
        AL11.alGenBuffers(buffers);

        for(int i = 0; i < BUFFER_COUNT * trackedPositions.size(); i++) {
            ShortBuffer silentBuffer = MemoryUtil.memAllocShort(0);
            silentBuffer.flip();

            AL11.alBufferData(buffers[i], AL11.AL_FORMAT_STEREO16, silentBuffer, sampleRate);
            AL11.alSourceQueueBuffers(sources[i / BUFFER_COUNT], buffers[i]);
            MemoryUtil.memFree(silentBuffer);
        }

        for(int source : sources) {
            AL11.alSourcePlay(source);
        }
        checkAlError();
    }

    public void addTrackedPosition(TrackedPosition pos) {
        ALC11.alcMakeContextCurrent(context);
        trackedPositions.add(pos);
        genSources();
        genBuffers();
    }

    public void removeTrackedPosition(TrackedPosition pos) {
        ALC11.alcMakeContextCurrent(context);
        trackedPositions.remove(pos);
        genSources();
        genBuffers();
    }

    public void updateListener() {
        ALC11.alcMakeContextCurrent(context);
        if(mc.cameraEntity == null) {
            EmuMod.LOGGER.error("Camera not present");
            return;
        }
        Vec3d pos = mc.cameraEntity.getLastRenderPos();
        //Vec3d vel = mc.player.getVelocity();
        Vec3d lookVec = mc.cameraEntity.getRotationVecClient();
        float[] orientation = {
                (float)lookVec.x, (float)lookVec.y, (float)lookVec.z,
                0.0f, 1.0f, 0.0f
        };
        AL11.alListener3f(AL11.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
        //AL11.alListener3f(AL11.AL_VELOCITY, (float) vel.x, (float) vel.y, (float) vel.z);
        AL11.alListenerfv(AL11.AL_ORIENTATION, orientation);
    }

    public void playAudio(short[] audioData) {
        ALC11.alcMakeContextCurrent(context);
        for(int source : sources) {
            playAudio(source, audioData);
        }
    }

    private void playAudio(int source, short[] audioData) {
        int processedBuffers = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED);
        if(processedBuffers <= 0) {
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

        AL11.alBufferData(bufferID, AL11.AL_FORMAT_STEREO16, dataBuffer, sampleRate);
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
        ALC11.alcMakeContextCurrent(context);
        if(sources != null) {
            AL11.alSourceStopv(sources);
            AL11.alDeleteSources(sources);
        }
        if(buffers != null) {
            AL11.alDeleteBuffers(buffers);
        }
        if(context != 0) {
            ALC11.alcDestroyContext(context);
        }
        if(device != 0) {
            ALC11.alcCloseDevice(device);
        }
    }

    public static void checkAlError() {
        int error = AL11.alGetError();
        if(error == AL11.AL_NO_ERROR) {
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
