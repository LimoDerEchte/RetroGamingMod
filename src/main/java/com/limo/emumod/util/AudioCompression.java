package com.limo.emumod.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class AudioCompression {

    public static byte[] compressAudio(short[] audioSamples, int compressionLevel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(audioSamples.length * 2);
        for (short sample : audioSamples) {
            byteBuffer.putShort(sample);
        }
        byte[] audioBytes = byteBuffer.array();
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(compressionLevel);
        DeflaterOutputStream dos = new DeflaterOutputStream(str, deflater);
        try {
            dos.write(audioBytes);
            dos.finish();
            dos.close();
            return str.toByteArray();
        } finally {
            deflater.end();
        }
    }

    public static byte[] compressAudioLossy(short[] audioSamples, int bitReduction, int compressionLevel) throws IOException {
        short[] reducedSamples = reduceBitDepth(audioSamples, bitReduction);
        return compressAudio(reducedSamples, compressionLevel);
    }

    public static short[] reduceBitDepth(short[] samples, int bitReduction) {
        if (bitReduction <= 0 || bitReduction >= 16) {
            return samples.clone();
        }
        short[] result = new short[samples.length];
        int mask = 0xFFFF << bitReduction;

        for (int i = 0; i < samples.length; i++) {
            result[i] = (short)(samples[i] & mask);
        }
        return result;
    }

    public static short[] decompressAudio(byte[] compressedBytes, int expectedSampleCount) throws IOException {
        ByteArrayInputStream inStr = new ByteArrayInputStream(compressedBytes);
        Inflater inflater = new Inflater();
        try (InflaterInputStream iis = new InflaterInputStream(inStr, inflater)) {
            byte[] decompressedBytes = new byte[expectedSampleCount * 2];
            int bytesRead = iis.read(decompressedBytes);
            short[] audioSamples = new short[bytesRead / 2];
            ByteBuffer byteBuffer = ByteBuffer.wrap(decompressedBytes, 0, bytesRead);
            for (int i = 0; i < audioSamples.length; i++) {
                audioSamples[i] = byteBuffer.getShort();
            }
            return audioSamples;
        } finally {
            inflater.end();
        }
    }

    public static short[] decompressAudio(byte[] compressedBytes) throws IOException {
        ByteArrayInputStream inStr = new ByteArrayInputStream(compressedBytes);
        Inflater inflater = new Inflater();
        try (InflaterInputStream iis = new InflaterInputStream(inStr, inflater); ByteArrayOutputStream str = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = iis.read(buffer)) != -1) {
                str.write(buffer, 0, bytesRead);
            }
            byte[] decompressedBytes = str.toByteArray();
            short[] audioSamples = new short[decompressedBytes.length / 2];
            ByteBuffer byteBuffer = ByteBuffer.wrap(decompressedBytes);
            for (int i = 0; i < audioSamples.length; i++) {
                audioSamples[i] = byteBuffer.getShort();
            }
            return audioSamples;
        } finally {
            inflater.end();
        }
    }
}
