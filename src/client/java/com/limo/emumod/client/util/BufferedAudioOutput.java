package com.limo.emumod.client.util;

import com.limo.emumod.EmuMod;
import net.minecraft.entity.TrackedPosition;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.limo.emumod.client.EmuModClient.mc;

public class BufferedAudioOutput {
    private long device;
    private long context;
    private int[] buffers;
    private int[] sources;
    private static final int BUFFER_COUNT = 5;

    private final int sampleRate;
    private final List<TrackedPosition> trackedPositions = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        context = ALC11.alcCreateContext(device, (IntBuffer) null);
        if(context == 0) {
            ALC11.alcCloseDevice(device);
            throw new RuntimeException("Failed to create OpenAL context");
        }
        runInContext(executor, () -> {
            AL.createCapabilities(ALC.createCapabilities(device));
            AL11.alDistanceModel(AL11.AL_LINEAR_DISTANCE);
            checkAlError();
            genSources();
            genBuffers();
        });
    }

    private void genSources() {
        sources = new int[trackedPositions.size()];
        AL11.alGenSources(sources);
        for (int source : sources) {
            AL11.alSourcei(source, AL11.AL_LOOPING, AL11.AL_FALSE);
            AL11.alSourcef(source, AL11.AL_REFERENCE_DISTANCE, 1.0f);
            AL11.alSourcef(source, AL11.AL_MAX_DISTANCE, 16.0f);
        }
        int error = AL11.alGetError();
        if(error != AL11.AL_NO_ERROR) {
            cleanup();
            throw new RuntimeException("OpenAL error: " + error);
        }
    }

    private void genBuffers() {
        AL11.alSourceStopv(sources);
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
        AL11.alSourcePlayv(sources);
        checkAlError();
    }

    public void addTrackedPosition(TrackedPosition pos) {
        runInContext(executor, () -> {
            trackedPositions.add(pos);
            genSources();
            genBuffers();
        });
    }

    public void removeTrackedPosition(TrackedPosition pos) {
        runInContext(executor, () -> {
            trackedPositions.remove(pos);
            genSources();
            genBuffers();
        });
    }

    public void updatePositions() {
        runInContext(executor, () -> {
            // Listener
            if(mc.cameraEntity == null) {
                EmuMod.LOGGER.error("Camera not present");
                return;
            }
            Vec3d pos = mc.cameraEntity.getLastRenderPos();
            Vec3d lookVec = mc.cameraEntity.getRotationVector();
            Vec3d upVec = lookVec.crossProduct(Direction.UP.getDoubleVector());
            float[] orientation = {
                    (float)lookVec.x, (float)lookVec.y, (float)lookVec.z,
                    (float)upVec.x, (float)upVec.y, (float)upVec.z
            };
            AL11.alListenerf(AL11.AL_GAIN, 1F);
            checkAlError();
            AL11.alListener3f(AL11.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
            checkAlError();
            AL11.alListenerfv(AL11.AL_ORIENTATION, orientation);
            checkAlError();
            AL11.alDistanceModel(AL11.AL_LINEAR_DISTANCE);
            checkAlError();
            // Sources
            checkAlError();
            for(int i = 0; i < sources.length; i++) {
                Vec3d tPos = trackedPositions.get(i).getPos();
                AL11.alSourcef(sources[i], AL11.AL_MAX_DISTANCE, 16.0f);
                checkAlError();
                AL11.alSourcef(sources[i], AL11.AL_REFERENCE_DISTANCE, 1.0f);
                checkAlError();
                AL11.alSourcei(sources[i], AL11.AL_SOURCE_RELATIVE, AL11.AL_FALSE);
                checkAlError();
                AL11.alSource3f(sources[i], AL11.AL_POSITION, (float) tPos.x, (float) tPos.y, (float) tPos.z);
                checkAlError();
            }
        });
    }

    public void playAudio(short[] audioData) {
        runInContext(executor, () -> {
            for(int source : sources) {
                playAudio(source, audioData);
            }
        });
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
        runInContext(executor, () -> {
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
        });
    }

    /*
     * The following methods are inspired by the simple-voice-chat mod
     */

    public void runInContext(Executor executor, Runnable runnable) {
        long time = System.currentTimeMillis();
        executor.execute(() -> {
            long diff = System.currentTimeMillis() - time;
            if (diff > 20) {
                EmuMod.LOGGER.warn("Sound executor delay: {} ms!", diff);
            }
            if (openContext()) {
                runnable.run();
                closeContext();
            }
        });
    }

    public boolean openContext() {
        if (context == 0) {
            return false;
        }
        boolean success = EXTThreadLocalContext.alcSetThreadContext(context);
        checkAlcError(device);
        return success;
    }

    public void closeContext() {
        EXTThreadLocalContext.alcSetThreadContext(0L);
        checkAlcError(device);
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

    public static void checkAlcError(long device) {
        int error = ALC11.alcGetError(device);
        if (error == ALC11.ALC_NO_ERROR) {
            return;
        }
        StackTraceElement stack = Thread.currentThread().getStackTrace()[2];
        EmuMod.LOGGER.error("ALC error: {}.{}[{}] {}", stack.getClassName(), stack.getMethodName(), stack.getLineNumber(), switch (error) {
            case ALC11.ALC_INVALID_DEVICE -> "Invalid device";
            case ALC11.ALC_INVALID_CONTEXT -> "Invalid context";
            case ALC11.ALC_INVALID_ENUM -> "Invalid enum";
            case ALC11.ALC_INVALID_VALUE -> "Invalid value";
            case ALC11.ALC_OUT_OF_MEMORY -> "Out of memory";
            default -> "Unknown error";
        });
    }
}
