package com.limo.emumod.network;

import com.limo.emumod.cartridge.CartridgeItem;
import com.limo.emumod.components.GameComponent;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
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

import static com.limo.emumod.registry.EmuComponents.GAME;

public class ServerHandler {

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(C2S.CreateCartridgePayload.ID, (payload, ctx) -> {
            ServerPlayNetworking.send(ctx.player(), new S2C.CloseScreenPayload(payload.handle()));
            byte[] nameBytes;
            Item item;
            String fileExtension;
            switch (payload.type()) {
                case 0 -> {
                    if(payload.data().length >= 0x142)
                        nameBytes = Arrays.copyOfRange(payload.data(), 0x134, 0x142);
                    else
                        nameBytes = "Invalid".getBytes();
                    boolean isGBC = payload.data()[0x143] != 0;
                    item = isGBC ? EmuItems.GAMEBOY_COLOR_CARTRIDGE : EmuItems.GAMEBOY_CARTRIDGE;
                    fileExtension = isGBC? "gbc" : "gb";
                }
                case 1 -> {
                    if(payload.data().length >= 0xAC)
                        nameBytes = Arrays.copyOfRange(payload.data(), 0xA0, 0xAC);
                    else
                        nameBytes = "Invalid".getBytes();
                    item = EmuItems.GAMEBOY_ADVANCE_CARTRIDGE;
                    fileExtension = "gba";
                }
                case 2 -> {
                    if(payload.data().length >= 0x7ffE)
                        nameBytes = Arrays.copyOfRange(payload.data(), 0x7ffC, 0x7ffE);
                    else
                        nameBytes = "Invalid".getBytes();
                    item = EmuItems.GAME_GEAR_CARTRIDGE;
                    fileExtension = "gg";
                }
                case 3 -> {
                    if(payload.data().length >= 0xFFEF)
                        nameBytes = Arrays.copyOfRange(payload.data(), 0xFFE0, 0xFFEF);
                    else
                        nameBytes = "Invalid".getBytes();
                    item = EmuItems.NES_CARTRIDGE;
                    fileExtension = "nes";
                }
                default -> {
                    ctx.player().sendMessage(Text.literal("Invalid request"), true);
                    return;
                }
            }
            UUID fileUuid = UUID.randomUUID();
            String game = new String(nameBytes).replace("\u0000", "");
            //noinspection resource
            ctx.server().execute(() -> {
                File rom = FileUtil.idToFile(fileUuid, fileExtension);
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
                stack.applyComponentsFrom(ComponentMap.builder().add(GAME, new GameComponent(fileUuid, game)).build());
                ctx.player().getInventory().insertStack(stack);
                ctx.player().sendMessage(Text.translatable("gui.emumod.cartridge.success"), true);
            });
        });
    }
}
