package com.limo.emumod.client;

import com.limo.emumod.client.bridge.NativeClient;
import com.limo.emumod.client.network.ClientHandler;
import com.limo.emumod.client.network.ScreenManager;
import com.limo.emumod.client.network.SoundManager;
import com.limo.emumod.client.render.MonitorBlockEntityRenderer;
import com.limo.emumod.client.util.ClientFilePath;
import com.limo.emumod.registry.EmuBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

import static com.limo.emumod.EmuMod.REQUIREMENTS_MET;

public class EmuModClient implements ClientModInitializer {
    public static MinecraftClient mc;
    public static NativeClient CLIENT;

    @Override
    public void onInitializeClient() {
        mc = MinecraftClient.getInstance();
        ClientFilePath.init();
        ScreenManager.init();
        SoundManager.init();

        if(!REQUIREMENTS_MET)
            return;
        ClientHandler.init();
        BlockEntityRendererFactories.register(EmuBlockEntities.MONITOR, MonitorBlockEntityRenderer::new);
    }
}
