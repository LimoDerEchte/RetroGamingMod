package com.limo.emumod.registry;

import com.limo.emumod.cartridge.CartridgeItem;
import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.gameboy.GameboyItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;

public class EmuItems {
    public static final Item CARTRIDGE = register(new CartridgeItem(), ItemId.Registry.CARTRIDGE);
    public static final Item BROKEN_CARTRIDGE = register(new Item(new Item.Settings().maxCount(8).registryKey(ItemId.Registry.BROKEN_CARTRIDGE)), ItemId.Registry.BROKEN_CARTRIDGE);

    public static final Item GAMEBOY_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_CARTRIDGE), ItemId.Registry.GAMEBOY_CARTRIDGE);
    public static final Item GAMEBOY_COLOR_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_COLOR_CARTRIDGE), ItemId.Registry.GAMEBOY_COLOR_CARTRIDGE);
    public static final Item GAMEBOY_ADVANCE_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_ADVANCE_CARTRIDGE), ItemId.Registry.GAMEBOY_ADVANCE_CARTRIDGE);

    public static final Item GAMEBOY = register(new GameboyItem(ItemId.Registry.GAMEBOY), ItemId.Registry.GAMEBOY);
    public static final Item GAMEBOY_COLOR = register(new GameboyItem(ItemId.Registry.GAMEBOY_COLOR), ItemId.Registry.GAMEBOY_COLOR);
    public static final Item GAMEBOY_ADVANCE = register(new GameboyItem(ItemId.Registry.GAMEBOY_ADVANCE), ItemId.Registry.GAMEBOY_ADVANCE);

    public static final Item MONITOR = register(new BlockItem(EmuBlocks.MONITOR, new Item.Settings().maxCount(8).registryKey(ItemId.Registry.MONITOR)), ItemId.Registry.MONITOR);

    public static final ItemGroup MAIN_GROUP = register(FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.emumod.main"))
            .icon(GAMEBOY_ADVANCE::getDefaultStack)
            .build(), ItemId.Registry.MAIN_GROUP);

    public static Item register(Item item, RegistryKey<Item> registryKey) {
        return Registry.register(Registries.ITEM, registryKey.getValue(), item);
    }

    public static ItemGroup register(ItemGroup group, RegistryKey<ItemGroup> registryKey) {
        return Registry.register(Registries.ITEM_GROUP, registryKey.getValue(), group);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemId.Registry.MAIN_GROUP).register(group -> {
            group.add(CARTRIDGE);
            group.add(BROKEN_CARTRIDGE);

            group.add(GAMEBOY);
            group.add(GAMEBOY_COLOR);
            group.add(GAMEBOY_ADVANCE);

            group.add(MONITOR);
        });
    }
}
