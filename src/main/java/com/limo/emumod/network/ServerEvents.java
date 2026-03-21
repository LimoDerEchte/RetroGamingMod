package com.limo.emumod.network;

import com.limo.emumod.EmuMod;
import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.bridge.NativeServer;
import com.limo.emumod.util.FileUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NetworkUtils;

import java.util.Map;
import java.util.UUID;

import static com.limo.emumod.EmuMod.LOGGER;

public class ServerEvents {
    public static MinecraftServer mcs;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            mcs = server;
            FileUtil.init();

            int serverPort = server.getServerPort();
            if(serverPort == -1)
                serverPort = NetworkUtils.findLocalPort();

            String serverIp = server.getServerIp();
            if(serverIp == null || serverIp.isEmpty())
                serverIp = "127.0.0.1";

            if(!NativeServer.init(server.getMaxPlayerCount(), "0.0.0.0:" + serverPort, new String[] {
                    serverIp + ":" + serverPort  // TODO: Find good way to search for all exposed addresses
            })) {
                LOGGER.error("The native server failed to initialize!");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> {
            EmuMod.running.values().forEach(NativeGenericConsole::stop);
            EmuMod.running.clear();
            if (!NativeServer.deinit())
                LOGGER.warn("Something went wrong while deinitializing the native server!");
        });
        ServerPlayConnectionEvents.JOIN.register((_, sender, _) -> {
            sender.sendPacket(new S2C.EmuModTokenPayload(NativeServer.createToken()));
            for(Map.Entry<UUID, NativeGenericConsole> pair : EmuMod.running.entrySet()) {
                NativeGenericConsole console = pair.getValue();
                sender.sendPacket(new S2C.UpdateEmulatorPayload(pair.getKey(), console.getId(), console.getWidth(), console.getHeight(),
                        console.getVideoCodec().ordinal(), console.getAudioCodec().ordinal(), console.getSampleRate()));
            }
        });
    }
}
