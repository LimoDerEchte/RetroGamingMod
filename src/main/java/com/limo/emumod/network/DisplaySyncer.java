package com.limo.emumod.network;

import com.limo.emumod.bridge.NativeDisplay;
import com.limo.emumod.gameboy.GameboyItem;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

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
            // Gameboy Screens
            GameboyItem.running.forEach((uuid, gb) -> {
                NativeDisplay display = gb.createDisplay();
                S2C.UpdateDisplayDataPayload pl = new S2C.UpdateDisplayDataPayload(uuid, display.getBuf().length == 38_400 ?
                        NetworkId.DisplaySize.w240h160 : NetworkId.DisplaySize.w160h144, display.getBuf());
                for(ServerPlayerEntity player : PlayerLookup.all(server)) {
                    ServerPlayNetworking.send(player, pl);
                }
            });
        }
    }

    public static void stop() {
        running = false;
    }
}
