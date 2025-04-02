package com.limo.emumod.client.network;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.limo.emumod.EmuMod.UUID_ZERO;
import static com.limo.emumod.client.EmuModClient.CLIENT;
import static com.limo.emumod.client.EmuModClient.mc;

public class SoundManager {
    private static final Map<UUID, UUID> fileEntityMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> fileDistanceMap = new ConcurrentHashMap<>();

    public static void init() {
        WorldRenderEvents.START.register((ctx) -> fileDistanceMap.clear());
        WorldRenderEvents.END.register((ctx) -> {
            ctx.world().getEntities().forEach(entity -> {
                double distance = mc.cameraEntity == null ? 0 : mc.cameraEntity.getPos().distanceTo(entity.getPos());
                SoundManager.updateEntityInRender(entity.getUuid(), distance);
            });
            fileDistanceMap.forEach((uuid, val) -> {
                if(CLIENT != null)
                    CLIENT.updateAudioDistance(uuid, val);
            });
        });
    }

    public static void updateEntity(UUID file, UUID entity) {
        if(entity.equals(UUID_ZERO)) {
            fileEntityMap.remove(file);
            return;
        }
        fileEntityMap.put(file, entity);
    }

    public static void updateInRender(UUID file, double distance) {
        fileDistanceMap.put(file, Math.max(distance, fileDistanceMap.getOrDefault(file, 0D)));
    }

    public static void updateEntityInRender(UUID entity, double distance) {
        for(Map.Entry<UUID, UUID> entry : fileEntityMap.entrySet()) {
            if(entry.getValue().equals(entity)) {
                updateInRender(entry.getKey(), distance);
            }
        }
    }
}
