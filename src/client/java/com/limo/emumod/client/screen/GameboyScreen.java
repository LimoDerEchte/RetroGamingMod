package com.limo.emumod.client.screen;

import com.limo.emumod.client.network.ScreenManager;
import com.limo.emumod.client.util.ControlHandler;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GameboyScreen extends Screen {
    private static final Identifier GAMEBOY_TEXTURE = Identifier.fromNamespaceAndPath("emumod", "textures/item/gameboy.png");
    private static final Identifier GAMEBOY_COLOR_TEXTURE = Identifier.fromNamespaceAndPath("emumod", "textures/item/gameboy_color.png");
    private static final int scale = 1;
    private static final Map<Integer, Short> inputMap = Map.of(
            GLFW.GLFW_KEY_I, (short) 0b1, // B
            GLFW.GLFW_KEY_RIGHT_SHIFT, (short) 0b100, // Select
            GLFW.GLFW_KEY_ENTER, (short) 0b1000, // Start
            GLFW.GLFW_KEY_W, (short) 0b10000, // Up
            GLFW.GLFW_KEY_S, (short) 0b100000, // Down
            GLFW.GLFW_KEY_A, (short) 0b1000000, // Left
            GLFW.GLFW_KEY_D, (short) 0b10000000, // Right
            GLFW.GLFW_KEY_J, (short) 0b100000000 // A
    );

    private final ControlHandler controlHandler;

    public boolean isGbc;
    public int streamId;

    public GameboyScreen(boolean isGbc, int streamId) {
        super(Component.nullToEmpty("Gameboy"));
        this.controlHandler = new ControlHandler(inputMap, streamId, (short) 0);
        this.isGbc = isGbc;
        this.streamId = streamId;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // License
        context.drawString(font, Component.translatable("gui.emumod.emulator.license_1",
                "GPL-3.0"), 10, height - 25, Color.WHITE.getRGB(), true);
        context.drawString(font, Component.translatable("gui.emumod.emulator.license_2",
                "https://github.com/drhelius/Gearboy/blob/master/LICENSE"), 10, height - 15, Color.WHITE.getRGB(), true);
        // Render Actual Stuff
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        // Background
        context.blit(RenderPipelines.GUI_TEXTURED, isGbc ? GAMEBOY_COLOR_TEXTURE : GAMEBOY_TEXTURE,
                (width / 2 - 512 / 2) / scale, (height / 2 - 144 - 64) / scale, 0, 0, 512, 512,
                32, 32, 32, 32);
        // Frame
        ScreenManager.retrieveDisplay(streamId);
        context.blit(RenderPipelines.GUI_TEXTURED, ScreenManager.texFromId(streamId), (width / 2 - 80) / scale,
                (height / 2 - 144) / scale, 0, 0, 160, 144, 160, 144);
        context.pose().popMatrix();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if(controlHandler.down(input.input()))
            return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if(controlHandler.up(input.input()))
            return true;
        return super.keyReleased(input);
    }
}
