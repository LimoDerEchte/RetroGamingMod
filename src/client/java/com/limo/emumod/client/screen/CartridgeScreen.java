package com.limo.emumod.client.screen;

import com.limo.emumod.EmuMod;
import com.limo.emumod.network.C2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.limo.emumod.client.EmuModClient.mc;

public class CartridgeScreen extends Screen {
    public TextFieldWidget fileInput;
    public Text failMessage;
    public int handle;

    public CartridgeScreen() {
        super(Text.translatable("gui.emumod.cartridge.title"));
        handle = EmuMod.RANDOM.nextInt();
    }

    @Override
    protected void init() {
        fileInput = addDrawableChild(new TextFieldWidget(textRenderer, width / 2 - 150, height / 2 - 20, 300, 20, Text.of("File")));
        fileInput.setMaxLength(300);
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.emumod.cartridge.create"), button -> {
            ButtonWidget.playClickSound(mc.getSoundManager());
            if(!fileInput.getText().isEmpty()) {
                File file = new File(fileInput.getText());
                if(!file.exists()) {
                    failMessage = Text.translatable("gui.emumod.cartridge.file_not_found");
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
                    failMessage = Text.translatable("gui.emumod.cartridge.file_read_error");
                }
            }
        }).dimensions(width / 2 - 75, height / 2 + 10, 150, 20).build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.emumod.cartridge.title"), width / 2, height / 2 - 40, Color.WHITE.getRGB());
        if(failMessage != null)
            ctx.drawCenteredTextWithShadow(textRenderer, failMessage, width / 2, height / 2 + 35, Color.RED.getRGB());
        super.render(ctx, mouseX, mouseY, delta);
    }
}
