package com.limo.emumod.client.util;

import com.limo.emumod.EmuMod;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.openal.AL11;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class BufferedAudioOutput {
    private final List<short[]> bufList = new ArrayList<>();

    private final int source;

    public BufferedAudioOutput() {
        source = AL11.alGenSources();
        checkAlError();
        int[] bufferIds = new int[3];
        AL11.alGenBuffers(bufferIds);
        checkAlError();
        for(int id : bufferIds) {
            AL11.alBufferData(id, AL11.AL_FORMAT_STEREO16, ShortBuffer.allocate(8192), 44100);
            checkAlError();
            AL11.alSourceQueueBuffers(source, id);
            checkAlError();
        }
        AL11.alSourcePlay(source);
        checkAlError();
        new Thread(this::mainLoop).start();
    }

    private void mainLoop() {
        while (true) {
            if(bufList.isEmpty())
                continue;
            MinecraftClient.getInstance().execute(this::updateStream);
        }
    }

    public void appendBuffer(short[] buf) {
        bufList.add(buf);
    }

    public void updateStream() {
        if(bufList.isEmpty())
            return;
        IntBuffer processedBuffer = IntBuffer.allocate(1);
        AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED, processedBuffer);
        checkAlError();
        while (processedBuffer.get(0) > 0) {
            int bufferId = AL11.alSourceUnqueueBuffers(source);
            checkAlError();
            AL11.alBufferData(bufferId, AL11.AL_FORMAT_MONO16, retrieveNextBuffer(), 44100);
            checkAlError();
            AL11.alSourceQueueBuffers(source, bufferId);
            checkAlError();
            processedBuffer.put(0, processedBuffer.get(0) - 1);
        }
    }

    private ShortBuffer retrieveNextBuffer() {
        return ShortBuffer.wrap(bufList.removeFirst());
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
