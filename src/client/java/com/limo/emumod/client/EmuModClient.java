package com.limo.emumod.client;

import com.limo.emumod.client.bridge.NativeClient;
import com.limo.emumod.client.network.ClientHandler;
import com.limo.emumod.client.render.MonitorBlockEntityRenderer;
import com.limo.emumod.client.util.ClientFilePath;
import com.limo.emumod.registry.EmuBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class EmuModClient implements ClientModInitializer {
    public static MinecraftClient mc;
    public static NativeClient CLIENT;

    @Override
    public void onInitializeClient() {
        mc = MinecraftClient.getInstance();
        ClientFilePath.init();
        ClientHandler.init();

        BlockEntityRendererFactories.register(EmuBlockEntities.MONITOR, MonitorBlockEntityRenderer::new);
    }
}
