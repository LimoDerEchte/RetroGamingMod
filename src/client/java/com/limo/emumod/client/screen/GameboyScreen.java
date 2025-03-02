package com.limo.emumod.client.screen;

import com.limo.emumod.client.network.ClientHandler;
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
            GLFW.GLFW_KEY_W, (short) 0b1, // D-Pad Up
            GLFW.GLFW_KEY_A, (short) 0b10, // D-Pad Left
            GLFW.GLFW_KEY_S, (short) 0b100, // D-Pad Down
            GLFW.GLFW_KEY_D, (short) 0b1000, // D-Pad Right
            GLFW.GLFW_KEY_J, (short) 0b10000, // A
            GLFW.GLFW_KEY_I, (short) 0b100000, // B
            GLFW.GLFW_KEY_ENTER, (short) 0b100000000, // Start
            GLFW.GLFW_KEY_RIGHT_SHIFT, (short) 0b1000000000 // Select
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
        if(ClientHandler.displayBuffer.containsKey(fileId)) {
            int[] display = ClientHandler.displayBuffer.get(fileId);
            for(int y = 0; y < 144; y++) {
                for(int x = 0; x < 160; x++) {
                    Objects.requireNonNull(frameTexture.getImage()).setColorArgb(x, y, display[y * 160 + x]);
                }
            }
            frameTexture.upload();
        }
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        controlHandler.down(keyCode);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        controlHandler.up(keyCode);
        return true;
    }
}
