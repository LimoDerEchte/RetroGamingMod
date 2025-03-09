package com.limo.emumod.client.network;

import com.limo.emumod.bridge.NativeDisplay;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.texture.NativeImage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.limo.emumod.client.EmuModClient.CLIENT;

public class ScreenManager {
    private static final Map<UUID, NativeDisplay> displays = new HashMap<>();
    private static final Map<UUID, NativeImage> displayBuffer = new HashMap<>();

    public static void init() {
        WorldRenderEvents.START.register((ctx) -> {
            for(Map.Entry<UUID, NativeDisplay> entry : displays.entrySet()) {
                if(!displayBuffer.containsKey(entry.getKey()))
                    continue;
                if(!entry.getValue().hasChanged())
                    continue;
                NativeImage img = displayBuffer.get(entry.getKey());
                int[] display = entry.getValue().getBuf();
                int height = img.getHeight();
                int width = img.getWidth();
                for(int y = 0; y < height; y++) {
                    for(int x = 0; x < width; x++) {
                        img.setColorArgb(x, y, display[y * width + x]);
                    }
                }
            }
        });
    }

    public static void registerDisplay(UUID id, int width, int height) {
        displays.put(id, CLIENT.registerScreen(id, width, height));
        displayBuffer.put(id, new NativeImage(width, height, false));
    }

    public static void unregisterDisplay(UUID id) {
        displays.remove(id);
        if(displayBuffer.containsKey(id)) {
            displayBuffer.get(id).close();
            displayBuffer.remove(id);
        }
        CLIENT.unregisterScreen(id);
    }

    public static NativeImage getDisplay(UUID id) {
        return displayBuffer.getOrDefault(id, new NativeImage(1, 1, false));
    }
}
