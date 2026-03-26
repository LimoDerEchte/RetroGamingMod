package com.limo.emumod.client.screen;

import com.limo.emumod.client.util.ControlHandler;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

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

    public RawControllerScreen(int streamId, short port) {
        super(Component.empty());
        controlHandler = new ControlHandler(inputMap, streamId, port);
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

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Nope
    }
}
