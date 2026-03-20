package com.limo.emumod.client.network;

import com.limo.emumod.client.bridge.NativeClient;
import com.limo.emumod.util.AudioCodec;
import com.limo.emumod.util.VideoCodec;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.limo.emumod.client.EmuModClient.mc;

public class ScreenManager {
    private static final Map<Integer, NativeImageBackedTexture> displays = new ConcurrentHashMap<>();
    private static final List<Integer> updatedThisFrame = new ArrayList<>();

    private static final Map<UUID, Integer> consoleStreamMapping = new ConcurrentHashMap<>();

    public static void init() {
        WorldRenderEvents.START_MAIN.register((_) -> updatedThisFrame.clear());
    }

    public static void registerDisplay(UUID console, int id, int width, int height, VideoCodec videoCodec, AudioCodec audioCodec) {
        consoleStreamMapping.put(console, id);
        unregisterDisplay(id);
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "emu-dp-" + id,
                NativeClient.registerScreen(id, width, height, videoCodec, audioCodec));
        displays.put(id, tex);
        mc.getTextureManager().registerTexture(texFromId(id), tex);
    }

    public static void unregisterDisplay(int id) {
        if(!displays.containsKey(id))
            return;
        displays.remove(id);
        NativeClient.unregisterScreen(id);
        RenderSystem.queueFencedTask(() -> mc.getTextureManager().destroyTexture(texFromId(id)));
    }

    public static NativeImageBackedTexture retrieveDisplay(UUID uuid) {
        if(!consoleStreamMapping.containsKey(uuid))
            return null;
        return retrieveDisplay(consoleStreamMapping.get(uuid));
    }

    public static NativeImageBackedTexture retrieveDisplay(int id) {
        if(!displays.containsKey(id))
            return null;
        if(updatedThisFrame.contains(id))
            return displays.get(id);

        NativeImageBackedTexture tex = displays.get(id);
        updatedThisFrame.add(id);
        if(!NativeClient.screenChanged(id))
            return tex;
        tex.upload();
        return tex;
    }

    public static Identifier texFromId(int id) {
        return Identifier.of("emumod", "emu-dp-" + id);
    }

    public static Identifier texFromUUID(UUID uuid) {
        if(!consoleStreamMapping.containsKey(uuid))
            return null;
        return texFromId(consoleStreamMapping.get(uuid));
    }
}
