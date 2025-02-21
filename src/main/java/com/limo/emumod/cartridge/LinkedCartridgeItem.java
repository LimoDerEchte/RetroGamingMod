package com.limo.emumod.cartridge;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.*;

import java.util.List;
import java.util.UUID;

public class LinkedCartridgeItem extends Item {
    public static final ComponentType<String> GAME = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("emumod", "game"),
            ComponentType.<String>builder().codec(Codec.STRING).build());
    public static final ComponentType<UUID> FILE_ID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("emumod", "file_id"),
            ComponentType.<UUID>builder().codec(Uuids.CODEC).build());

    public LinkedCartridgeItem(RegistryKey<Item> key) {
        super(new Settings().maxCount(1).registryKey(key));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        gameTooltip(stack, tooltip);
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
