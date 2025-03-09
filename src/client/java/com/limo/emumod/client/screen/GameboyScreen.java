package com.limo.emumod.client.screen;

import com.limo.emumod.client.network.ScreenManager;
import com.limo.emumod.client.util.ControlHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GameboyScreen extends Screen {
    private static int texIncrement = 0;
    private static final Identifier GAMEBOY_TEXTURE = Identifier.of("emumod", "textures/gui/gameboy.png");
    private static final Identifier GAMEBOY_COLOR_TEXTURE = Identifier.of("emumod", "textures/gui/gameboy_color.png");
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
    private final NativeImageBackedTexture frameTexture;
    private final Identifier screenTexture;

    public boolean isGbc;
    public UUID fileId;

    public GameboyScreen(boolean isGbc, UUID fileId) {
        super(Text.of("Gameboy"));
        this.controlHandler = new ControlHandler(inputMap, fileId);
        this.frameTexture = new NativeImageBackedTexture(160, 144, false);
        this.screenTexture = Identifier.of("emumod", "gb_screen_" + texIncrement++);
        MinecraftClient.getInstance().getTextureManager().registerTexture(screenTexture, frameTexture);
        this.isGbc = isGbc;
        this.fileId = fileId;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // License
        context.drawText(textRenderer, Text.translatable("gui.emumod.emulator.license_1",
                "MPL-2.0"), 10, height - 25, Color.WHITE.getRGB(), true);
        context.drawText(textRenderer, Text.translatable("gui.emumod.emulator.license_2",
                "https://github.com/mgba-emu/mgba/blob/master/LICENSE"), 10, height - 15, Color.WHITE.getRGB(), true);
        // Update Texture
        Objects.requireNonNull(frameTexture.getImage()).copyFrom(ScreenManager.getDisplay(fileId));
        frameTexture.upload();
        // Render Actual Stuff
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, scale);
        // Background
        context.drawTexture(RenderLayer::getGuiTextured, isGbc ? GAMEBOY_COLOR_TEXTURE : GAMEBOY_TEXTURE,
                (width / 2 - 512 / 2) / scale, (height / 2 - 144 - 64) / scale, 0, 0, 512, 512, 512, 512);
        // Frame
        context.drawTexture(RenderLayer::getGuiTextured, screenTexture, (width / 2 - 80) / scale,
                (height / 2 - 144) / scale, 0, 0, 160, 144, 160, 144);
        context.getMatrices().pop();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(controlHandler.down(keyCode))
            return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(controlHandler.up(keyCode))
            return true;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
