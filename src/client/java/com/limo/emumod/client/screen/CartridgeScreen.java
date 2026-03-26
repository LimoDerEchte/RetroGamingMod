package com.limo.emumod.client.screen;

import com.limo.emumod.EmuMod;
import com.limo.emumod.network.C2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.limo.emumod.client.EmuModClient.mc;

public class CartridgeScreen extends Screen {
    public EditBox fileInput;
    public Component failMessage;
    public int handle;

    public CartridgeScreen() {
        super(Component.translatable("gui.emumod.cartridge.title"));
        handle = EmuMod.RANDOM.nextInt();
    }

    @Override
    protected void init() {
        fileInput = addRenderableWidget(new EditBox(font, width / 2 - 150, height / 2 - 20, 300, 20, Component.nullToEmpty("File")));
        fileInput.setMaxLength(300);
        addRenderableWidget(Button.builder(Component.translatable("gui.emumod.cartridge.create"), button -> {
            Button.playButtonClickSound(mc.getSoundManager());
            if(!fileInput.getValue().isEmpty()) {
                File file = new File(fileInput.getValue());
                if(!file.exists()) {
                    failMessage = Component.translatable("gui.emumod.cartridge.file_not_found");
                    return;
                }
                try {
                    byte type = 0;
                    if(file.getName().endsWith(".gba"))
                        type = 1;
                    else if(file.getName().endsWith(".gg"))
                        type = 2;
                    else if(file.getName().endsWith(".nes"))
                        type = 3;
                    ClientPlayNetworking.send(new C2S.CreateCartridgePayload(handle, type, Files.readAllBytes(file.toPath())));
                } catch (IOException e) {
                    failMessage = Component.translatable("gui.emumod.cartridge.file_read_error");
                }
            }
        }).bounds(width / 2 - 75, height / 2 + 10, 150, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.drawCenteredString(font, Component.translatable("gui.emumod.cartridge.title"), width / 2, height / 2 - 40, Color.WHITE.getRGB());
        if(failMessage != null)
            ctx.drawCenteredString(font, failMessage, width / 2, height / 2 + 35, Color.RED.getRGB());
        super.render(ctx, mouseX, mouseY, delta);
    }
}
