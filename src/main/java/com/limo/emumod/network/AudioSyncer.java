package com.limo.emumod.network;

import com.limo.emumod.EmuMod;
import com.limo.emumod.bridge.NativeAudio;
import com.limo.emumod.bridge.NativeGameBoy;
import com.limo.emumod.gameboy.GameboyItem;
import com.limo.emumod.util.AudioCompression;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;

public class AudioSyncer {
    private static long lastTime = System.nanoTime();
    private static boolean running = true;

    /*private static final Map<UUID, ShortBuffer> bufferBuilders = new HashMap<>();
    private static final int bufferLengthOnSend = 4 * 1024;*/

    public static void run(MinecraftServer server) {
        running = true;
        int nqDelay = 1_000_000;
        while (running) {
            while (System.nanoTime() - lastTime < nqDelay) {
                Thread.onSpinWait();
            }
            lastTime = System.nanoTime();
            try {
                sendUpdates(server);
            } catch (IOException e) {
                EmuMod.LOGGER.error("Failed to send audio updates", e);
            }
        }
    }

    private static void sendUpdates(MinecraftServer server) throws IOException {
        // Gameboy Audio
        for(Map.Entry<UUID, NativeGameBoy> entry : GameboyItem.running.entrySet()) {
            NativeAudio audio = entry.getValue().createAudio();
            if(!audio.hasChanged())
                continue;
            /*if(!bufferBuilders.containsKey(entry.getKey()))
                bufferBuilders.put(entry.getKey(), ShortBuffer.allocate(bufferLengthOnSend));
            short[] data  = audio.getBuf();
            ShortBuffer buffer = bufferBuilders.get(entry.getKey());
            if(buffer.position() + data.length < bufferLengthOnSend) {
                buffer.put(data);
                continue;
            }
            buffer.flip();
            short[] sendData = new short[buffer.position() - 1];
            buffer.get(sendData);*/
            S2C.UpdateAudioDataPayload pl = new S2C.UpdateAudioDataPayload(entry.getKey(),
                    AudioCompression.compressAudioLossy(audio.getBuf(), 0, Deflater.NO_COMPRESSION));
            for(ServerPlayerEntity player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, pl);
            }
            //buffer.clear();
            //buffer.put(data);
        }
    }

    public static void stop() {
        running = false;
    }
}
