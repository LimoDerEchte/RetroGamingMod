package com.limo.emumod.util;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class VideoCompression {

    /**
     * Decompresses an int array into a byte array
     */
    public static byte[] compress(int[] data) throws IOException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        int[] deltaEncoded = new int[data.length];
        deltaEncoded[0] = data[0];
        for (int i = 1; i < data.length; i++) {
            deltaEncoded[i] = data[i] - data[i-1];
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        dataStream.writeInt(data.length);
        for (int delta : deltaEncoded) {
            dataStream.writeInt(delta);
        }
        dataStream.flush();
        byte[] byteArray = byteStream.toByteArray();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(byteArray);
        deflater.finish();
        ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            compressedStream.write(buffer, 0, count);
        }
        deflater.end();
        return compressedStream.toByteArray();
    }

    /**
     * Decompresses a byte array back to an integer array
     */
    public static int[] decompress(byte[] compressedData) throws IOException {
        if (compressedData == null || compressedData.length == 0) {
            return new int[0];
        }
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                byteStream.write(buffer, 0, count);
            }
        } catch (Exception e) {
            throw new IOException("Error during decompression", e);
        } finally {
            inflater.end();
        }
        DataInputStream dataStream = new DataInputStream(
                new ByteArrayInputStream(byteStream.toByteArray())
        );
        int length = dataStream.readInt();
        int[] deltaEncoded = new int[length];
        for (int i = 0; i < length; i++) {
            deltaEncoded[i] = dataStream.readInt();
        }
        int[] originalData = new int[length];
        originalData[0] = deltaEncoded[0];
        for (int i = 1; i < length; i++) {
            originalData[i] = originalData[i-1] + deltaEncoded[i];
        }
        return originalData;
    }
}
