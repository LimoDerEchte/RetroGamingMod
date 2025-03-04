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
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;

public class AudioSyncer {
    private static long lastTime = System.nanoTime();
    private static boolean running = true;

    public static void run(MinecraftServer server) {
        if(1==1)
        return;
        running = true;
        int nqDelay = 10_000_000;
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
            S2C.UpdateAudioDataPayload pl = new S2C.UpdateAudioDataPayload(entry.getKey(),
                    AudioCompression.compressAudioLossy(audio.getBuf(), 4, Deflater.BEST_COMPRESSION));
            for(ServerPlayerEntity player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, pl);
            }
        }
    }

    public static void stop() {
        running = false;
    }
}
