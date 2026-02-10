package com.limo.emumod.client.screen;

import com.limo.emumod.client.util.ControlHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.UUID;

public class RawControllerScreen extends Screen {

    private static final Map<Integer, Short> inputMap = Map.of(
            GLFW.GLFW_KEY_I, (short) 0b1, // B
            GLFW.GLFW_KEY_RIGHT_SHIFT, (short) 0b100, // Select
            GLFW.GLFW_KEY_ENTER, (short) 0b1000, // Start
            GLFW.GLFW_KEY_W, (short) 0b10000, // Up
            GLFW.GLFW_KEY_S, (short) 0b100000, // Down
            GLFW.GLFW_KEY_A, (short) 0b1000000, // Left
            GLFW.GLFW_KEY_D, (short) 0b10000000, // Right
            GLFW.GLFW_KEY_J, (short) 0b100000000, // A
            GLFW.GLFW_KEY_U, (short) 0b10000000000, // L
            GLFW.GLFW_KEY_O, (short) 0b100000000000 // R
    );
    private final ControlHandler controlHandler;

    public RawControllerScreen(UUID file, int port) {
        super(Text.empty());
        controlHandler = new ControlHandler(inputMap, file, port);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if(controlHandler.down(input.getKeycode()))
            return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        if(controlHandler.up(input.getKeycode()))
            return true;
        return super.keyReleased(input);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Nope
    }
}
