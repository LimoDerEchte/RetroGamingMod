package com.limo.emumod.network;

import com.limo.emumod.cartridge.CartridgeItem;
import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.gameboy.GameboyItem;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class ServerHandler {

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            FileUtil.init();
            new Thread(() -> DisplaySyncer.run(server)).start();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DisplaySyncer.stop();
            GameboyItem.running.forEach((ignore, nat) -> {
                nat.stop();
            });
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
                        .add(LinkedCartridgeItem.GAME, game)
                        .add(LinkedCartridgeItem.FILE_ID, fileUuid).build());
                ctx.player().getInventory().insertStack(stack);
                ctx.player().sendMessage(Text.translatable("gui.emumod.cartridge.success"), true);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S.UpdateGameControls.ID, (payload, ctx) -> {
            // Gameboys
            if(GameboyItem.running.containsKey(payload.uuid())) {
                GameboyItem.running.get(payload.uuid()).updateInput(payload.input());
            }
        });
    }
}
