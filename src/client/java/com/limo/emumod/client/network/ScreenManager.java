package com.limo.emumod.client.network;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.*;

import static com.limo.emumod.client.EmuModClient.CLIENT;
import static com.limo.emumod.client.EmuModClient.mc;

public class ScreenManager {
    private static final Map<UUID, NativeImageBackedTexture> displays = new HashMap<>();
    private static final List<UUID> updatedThisFrame = new ArrayList<>();

    public static void init() {
        WorldRenderEvents.START_MAIN.register((_) -> updatedThisFrame.clear());
    }

    public static void registerDisplay(UUID id, int width, int height, int sampleRate) {
        unregisterDisplay(id);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "emu-dp-" + id.toString(),
                CLIENT.registerScreen(id, width, height, sampleRate));
        displays.put(id, tex);
        mc.getTextureManager().registerTexture(texFromUUID(id), tex);
    }

    public static void unregisterDisplay(UUID id) {
        if(!displays.containsKey(id))
            return;
        CLIENT.unregisterScreen(id);
        mc.getTextureManager().destroyTexture(texFromUUID(id));
        displays.remove(id);
    }

    public static NativeImageBackedTexture retrieveDisplay(UUID id) {
        if(!displays.containsKey(id))
            return null;
        if(updatedThisFrame.contains(id))
            return displays.get(id);
        NativeImageBackedTexture tex = displays.get(id);
        updatedThisFrame.add(id);
        if(!CLIENT.screenChanged(id))
            return tex;
        tex.upload();
        return tex;
    }

    public static Identifier texFromUUID(UUID uuid) {
        return Identifier.of("emumod", "emu-dp-" + uuid.toString());
    }
}
