package com.limo.emumod.client;

import com.limo.emumod.client.network.ClientHandler;
import com.limo.emumod.client.util.ClientFilePath;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class EmuModClient implements ClientModInitializer {
    public static MinecraftClient mc;

    @Override
    public void onInitializeClient() {
        mc = MinecraftClient.getInstance();
        ClientFilePath.init();
        ClientHandler.init();
    }
}
