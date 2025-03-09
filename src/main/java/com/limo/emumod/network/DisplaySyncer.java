package com.limo.emumod.network;

import com.limo.emumod.EmuMod;
import com.limo.emumod.bridge.NativeDisplay;
import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.console.GenericHandheldItem;
import com.limo.emumod.util.VideoCompression;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class DisplaySyncer {
    private static final float fps = 30;
    private static long lastTime = System.nanoTime();
    private static boolean running = true;

    public static void run(MinecraftServer server) {
        running = true;
        int nqDelay = (int) (1_000_000_000 / fps);
        while (running) {
            while (System.nanoTime() - lastTime < nqDelay) {
                Thread.onSpinWait();
            }
            lastTime = System.nanoTime();
            try {
                sendUpdates(server);
            } catch (IOException e) {
                EmuMod.LOGGER.error("Failed to send display updates", e);
            }
        }
    }

    private static void sendUpdates(MinecraftServer server) throws IOException {
        // Gameboy Screens
        for(Map.Entry<UUID, NativeGenericConsole> entry : GenericHandheldItem.running.entrySet()) {
            NativeDisplay display = entry.getValue().createDisplay();
            if(!display.hasChanged())
                return;
            S2C.UpdateDisplayDataPayload pl = new S2C.UpdateDisplayDataPayload(entry.getKey(), display.getBuf().length == 38_400 ?
                    NetworkId.DisplaySize.w240h160 : NetworkId.DisplaySize.w160h144, VideoCompression.compress(display.getBuf()));
            for(ServerPlayerEntity player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, pl);
            }
        }
    }

    public static void stop() {
        running = false;
    }
}
