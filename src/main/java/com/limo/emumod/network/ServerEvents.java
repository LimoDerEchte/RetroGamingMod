package com.limo.emumod.network;

import com.limo.emumod.EmuMod;
import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.bridge.NativeServer;
import com.limo.emumod.console.GenericHandheldItem;
import com.limo.emumod.util.FileUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.ComponentMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NetworkUtils;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.limo.emumod.EmuMod.SERVER;
import static com.limo.emumod.EmuMod.UUID_ZERO;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class ServerEvents {
    public static MinecraftServer mcs;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            mcs = server;
            FileUtil.init();
            int serverPort = server.getServerPort();
            if(serverPort == -1)
                serverPort = NetworkUtils.findLocalPort();
            SERVER = new NativeServer(serverPort, server.getMaxPlayerCount());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            EmuMod.running.values().forEach(NativeGenericConsole::stop);
            EmuMod.running.clear();
            if(SERVER != null) {
                SERVER.stop();
                SERVER = null;
            }
        });
        ServerPlayConnectionEvents.JOIN.register((a, sender, server) -> {
            sender.sendPacket(new S2C.ENetTokenPayload(SERVER.getPort(), SERVER.createToken()));
            for(Map.Entry<UUID, NativeGenericConsole> console : EmuMod.running.entrySet()) {
                sender.sendPacket(new S2C.UpdateEmulatorPayload(console.getKey(),
                        console.getValue().getWidth(), console.getValue().getHeight(), console.getValue().getSampleRate()));
            }
        });
        ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, slot, previous, current) -> {
            ComponentMap pc = previous.getComponents();
            if(previous.getItem() instanceof GenericHandheldItem && pc.contains(GAME)) {
                PlayerLookup.all(mcs).forEach(player -> ServerPlayNetworking.send(player,
                        new S2C.UpdateHandheldAudio(Objects.requireNonNull(pc.get(GAME)).fileId(), UUID_ZERO)));
            }
            ComponentMap cc = current.getComponents();
            if(current.getItem() instanceof GenericHandheldItem && cc.contains(GAME) && entity.getUuid() != null) {
                PlayerLookup.all(mcs).forEach(player -> ServerPlayNetworking.send(player,
                        new S2C.UpdateHandheldAudio(Objects.requireNonNull(cc.get(GAME)).fileId(), entity.getUuid())));
            }
        });
    }
}
