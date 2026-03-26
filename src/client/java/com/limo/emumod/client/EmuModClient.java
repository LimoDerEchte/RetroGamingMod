package com.limo.emumod.client;

import com.limo.emumod.client.network.ClientHandler;
import com.limo.emumod.client.network.ScreenManager;
import com.limo.emumod.client.render.MonitorBlockEntityRenderer;
import com.limo.emumod.client.util.ClientFilePath;
import com.limo.emumod.registry.EmuBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import static com.limo.emumod.EmuMod.REQUIREMENTS_MET;

public class EmuModClient implements ClientModInitializer {
    public static Minecraft mc;

    @Override
    public void onInitializeClient() {
        mc = Minecraft.getInstance();
        ClientFilePath.init();
        ScreenManager.init();

        if(!REQUIREMENTS_MET)
            return;
        ClientHandler.init();
        BlockEntityRenderers.register(EmuBlockEntities.MONITOR, MonitorBlockEntityRenderer::new);
    }
}
