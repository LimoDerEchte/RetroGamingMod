package com.limo.emumod.cartridge;

import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.gameboy.GameboyItem;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static com.limo.emumod.registry.EmuComponents.FILE_ID;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class LinkedCartridgeItem extends Item {

    public LinkedCartridgeItem(RegistryKey<Item> key) {
        super(new Settings().maxCount(1).registryKey(key));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        gameTooltip(stack, tooltip);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient())
            return super.use(world, user, hand);
        ItemStack stack = user.getStackInHand(hand);
        ItemStack link = findLinkItem();
        if(link != null && link.getCount() > 0 && hasGame(stack)) {
            UUID id = stack.getComponents().get(FILE_ID);
            File file = FileUtil.idToFile(id, "cart");
            if(!file.exists()) {
                stack.setCount(0);
                user.getInventory().insertStack(new ItemStack(EmuItems.BROKEN_CARTRIDGE));
                user.sendMessage(Text.translatable("item.emumod.gameboy.file_deleted").formatted(Formatting.RED), true);
            } else {
                link.applyComponentsFrom(ComponentMap.builder()
                        .add(GAME, stack.getComponents().get(GAME))
                        .add(FILE_ID, stack.getComponents().get(FILE_ID)).build());
                clearLinkItem();
                stack.setCount(0);
                user.sendMessage(Text.translatable("item.emumod.gameboy.insert"), true);
                run(id);
            }
        }
        return ActionResult.PASS;
    }

    private ItemStack findLinkItem() {
        if(EmuItems.GAMEBOY_CARTRIDGE == this)
            return GameboyItem.link != null && GameboyItem.link.getItem() == EmuItems.GAMEBOY ? GameboyItem.link : ItemStack.EMPTY;
        if(EmuItems.GAMEBOY_COLOR_CARTRIDGE == this)
            return GameboyItem.link != null && GameboyItem.link.getItem() == EmuItems.GAMEBOY_COLOR ? GameboyItem.link : ItemStack.EMPTY;
        if(EmuItems.GAMEBOY_ADVANCE_CARTRIDGE == this)
            return GameboyItem.link != null && GameboyItem.link.getItem() == EmuItems.GAMEBOY_ADVANCE ? GameboyItem.link : ItemStack.EMPTY;
        return ItemStack.EMPTY;
    }

    private void clearLinkItem() {
        if(EmuItems.GAMEBOY_CARTRIDGE == this || EmuItems.GAMEBOY_COLOR_CARTRIDGE == this || EmuItems.GAMEBOY_ADVANCE_CARTRIDGE == this)
            GameboyItem.link = null;
    }

    private void run(UUID id) {
        if(EmuItems.GAMEBOY_CARTRIDGE == this || EmuItems.GAMEBOY_COLOR_CARTRIDGE == this || EmuItems.GAMEBOY_ADVANCE_CARTRIDGE == this) {
            boolean gba = EmuItems.GAMEBOY_ADVANCE_CARTRIDGE == this;
            NativeGenericConsole gb = new NativeGenericConsole(
                    gba ? 240 : 160,
                    gba ? 160 : 144
            );
            gb.load(id);
            GameboyItem.running.put(id, gb);
        }
    }

    public static void gameTooltip(ItemStack stack, List<Text> tooltip) {
        ComponentMap components = stack.getComponents();
        if(components.contains(FILE_ID) && components.contains(GAME)) {
            tooltip.add(Text.translatable("item.emumod.tooltip.game", components.get(GAME)).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("item.emumod.tooltip.file", components.get(FILE_ID)).formatted(Formatting.GRAY));
        }
    }

    public static boolean hasGame(ItemStack stack) {
        ComponentMap components = stack.getComponents();
        return components.contains(FILE_ID) && components.contains(GAME);
    }
}
