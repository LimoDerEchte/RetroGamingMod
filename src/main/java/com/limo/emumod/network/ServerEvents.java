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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NetworkUtils;

import java.util.Map;
import java.util.UUID;

import static com.limo.emumod.EmuMod.SERVER;
import static com.limo.emumod.EmuMod.UUID_ZERO;
import static com.limo.emumod.registry.EmuComponents.FILE_ID;

public class ServerEvents {
    public static MinecraftServer mcs;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            mcs = server;
            FileUtil.init();
            int serverPort = server.getServerPort();
            if(serverPort == -1)
                serverPort = NetworkUtils.findLocalPort();
            SERVER = new NativeServer(serverPort);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> {
            EmuMod.running.values().forEach(NativeGenericConsole::stop);
            EmuMod.running.clear();
            if(SERVER != null) {
                SERVER.stop();
                SERVER = null;
            }
        });
        ServerPlayConnectionEvents.JOIN.register((a, sender, _) -> {
            sender.sendPacket(new S2C.ENetTokenPayload(SERVER.getPort(), SERVER.createToken()));
            for(Map.Entry<UUID, NativeGenericConsole> console : EmuMod.running.entrySet()) {
                sender.sendPacket(new S2C.UpdateEmulatorPayload(console.getKey(),
                        console.getValue().getWidth(), console.getValue().getHeight(), console.getValue().getSampleRate()));
            }
        });
        ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, _, previous, current) -> {
            if(previous.getItem() instanceof GenericHandheldItem && previous.hasChangedComponent(FILE_ID)) {
                PlayerLookup.all(mcs).forEach(player -> ServerPlayNetworking.send(player,
                        new S2C.UpdateHandheldAudio(current.getComponents().get(FILE_ID), UUID_ZERO)));
            }
            if(current.getItem() instanceof GenericHandheldItem && current.hasChangedComponent(FILE_ID)) {
                PlayerLookup.all(mcs).forEach(player -> ServerPlayNetworking.send(player,
                        new S2C.UpdateHandheldAudio(current.getComponents().get(FILE_ID), entity.getUuid())));
            }
        });
    }
}
