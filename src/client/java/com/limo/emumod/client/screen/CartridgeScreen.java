package com.limo.emumod.client.screen;

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

public class CartridgeScreen extends Screen {
    public TextFieldWidget fileInput;
    public Text failMessage;

    public CartridgeScreen() {
        super(Text.translatable("gui.cartridge.title"));
    }

    public void setFailMessage(String id) {
        this.failMessage = Text.translatable("gui.cartridge." + id);
    }

    @Override
    protected void init() {
        fileInput = addDrawableChild(new TextFieldWidget(textRenderer, width / 2 - 150, height / 2 - 20, 300, 20, Text.of("File")));
        fileInput.setMaxLength(300);
        addDrawable(ButtonWidget.builder(Text.translatable("gui.emumod.cartridge.create"), button -> {
            if(!fileInput.getText().isEmpty()) {
                File file = new File(fileInput.getText());
                if(!file.exists()) {
                    failMessage = Text.translatable("gui.emumod.cartridge.file_not_found");
                    return;
                }
                try {
                    ClientPlayNetworking.send(new C2S.CreateCartridgePayload(Files.readAllBytes(file.toPath())));
                } catch (IOException e) {
                    failMessage = Text.translatable("gui.emumod.cartridge.file_read_error");
                }
            }
        }).dimensions(width / 2 - 75, height / 2 + 10, 150, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.emumod.cartridge.title"), width / 2, height / 2 - 40, Color.WHITE.getRGB());
        if(failMessage != null)
            ctx.drawCenteredTextWithShadow(textRenderer, failMessage, width / 2, height / 2 + 35, Color.RED.getRGB());
        super.render(ctx, mouseX, mouseY, delta);
    }
}
