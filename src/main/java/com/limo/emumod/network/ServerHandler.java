package com.limo.emumod.network;

import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.bridge.NativeServer;
import com.limo.emumod.cartridge.CartridgeItem;
import com.limo.emumod.console.GenericHandheldItem;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.NetworkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static com.limo.emumod.EmuMod.SERVER;
import static com.limo.emumod.registry.EmuComponents.FILE_ID;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class ServerHandler {
    public static MinecraftServer mcs;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            mcs = server;
            FileUtil.init();
            new Thread(() -> AudioSyncer.run(server)).start();
            SERVER = new NativeServer(NetworkUtils.findLocalPort());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            AudioSyncer.stop();
            GenericHandheldItem.running.values().forEach(NativeGenericConsole::stop);
            GenericHandheldItem.running.clear();
            if(SERVER != null) {
                SERVER.stop();
                SERVER = null;
            }
        });
        ServerPlayConnectionEvents.JOIN.register((a, sender, server) -> {
            sender.sendPacket(new S2C.ENetTokenPayload(SERVER.getPort(), SERVER.createToken()));
            for(Map.Entry<UUID, NativeGenericConsole> console : GenericHandheldItem.running.entrySet()) {
                sender.sendPacket(new S2C.UpdateDisplayPayload(console.getKey(),
                        console.getValue().getWidth(), console.getValue().getHeight()));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S.CreateCartridgePayload.ID, (payload, ctx) -> {
            ServerPlayNetworking.send(ctx.player(), new S2C.CloseScreenPayload(payload.handle()));
            byte[] nameBytes;
            Item item;
            switch (payload.type()) {
                case 0 -> {
                    nameBytes = Arrays.copyOfRange(payload.data(), 0x134, 0x142);
                    item = payload.data()[0x143] != 0 ? EmuItems.GAMEBOY_COLOR_CARTRIDGE : EmuItems.GAMEBOY_CARTRIDGE;
                }
                case 1 -> {
                    nameBytes = Arrays.copyOfRange(payload.data(), 0xA0, 0xAC);
                    item = EmuItems.GAMEBOY_ADVANCE_CARTRIDGE;
                }
                default -> {
                    ctx.player().sendMessage(Text.literal("Invalid request"), true);
                    return;
                }
            }
            UUID fileUuid = UUID.randomUUID();
            String game = new String(nameBytes).replace("\u0000", "");
            ctx.server().execute(() -> {
                File rom = FileUtil.idToFile(fileUuid, "cart");
                try(FileOutputStream fos = new FileOutputStream(rom)) {
                    fos.write(payload.data());
                    fos.flush();
                } catch (IOException e) {
                    ctx.player().sendMessage(Text.translatable("gui.emumod.cartridge.file_write_error"), true);
                    return;
                }
                if(!(ctx.player().getMainHandStack().getItem() instanceof CartridgeItem)) {
                    ctx.player().sendMessage(Text.translatable("gui.emumod.cartridge.item_unavailable"), true);
                    return;
                }
                ctx.player().getMainHandStack().decrement(1);
                ItemStack stack = new ItemStack(item);
                stack.applyComponentsFrom(ComponentMap.builder()
                        .add(GAME, game)
                        .add(FILE_ID, fileUuid).build());
                ctx.player().getInventory().insertStack(stack);
                ctx.player().sendMessage(Text.translatable("gui.emumod.cartridge.success"), true);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S.UpdateGameControls.ID, (payload, ctx) -> {
            // Gameboys
            if(GenericHandheldItem.running.containsKey(payload.uuid())) {
                GenericHandheldItem.running.get(payload.uuid()).updateInput(payload.port(), payload.input());
            }
        });
    }
}
