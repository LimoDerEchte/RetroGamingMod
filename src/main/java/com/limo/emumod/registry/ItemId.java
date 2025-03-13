package com.limo.emumod.registry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ItemId {

    public static class Id {
        public static final Identifier CARTRIDGE = Identifier.of("emumod", "cartridge");
        public static final Identifier BROKEN_CARTRIDGE = Identifier.of("emumod", "broken_cartridge");

        public static final Identifier GAMEBOY_CARTRIDGE = Identifier.of("emumod", "gameboy_cartridge");
        public static final Identifier GAMEBOY_COLOR_CARTRIDGE = Identifier.of("emumod", "gameboy_color_cartridge");
        public static final Identifier GAMEBOY_ADVANCE_CARTRIDGE = Identifier.of("emumod", "gameboy_advance_cartridge");
        public static final Identifier GAME_GEAR_CARTRIDGE = Identifier.of("emumod", "game_gear_cartridge");

        public static final Identifier GAMEBOY = Identifier.of("emumod", "gameboy");
        public static final Identifier GAMEBOY_COLOR = Identifier.of("emumod", "gameboy_color");
        public static final Identifier GAMEBOY_ADVANCE = Identifier.of("emumod", "gameboy_advance");
        public static final Identifier GAME_GEAR = Identifier.of("emumod", "game_gear");

        public static final Identifier MAIN_GROUP = Identifier.of("emumod", "main_group");
    }

    public static class Registry {
        public static final RegistryKey<Item> CARTRIDGE = RegistryKey.of(RegistryKeys.ITEM, Id.CARTRIDGE);
        public static final RegistryKey<Item> BROKEN_CARTRIDGE = RegistryKey.of(RegistryKeys.ITEM, Id.BROKEN_CARTRIDGE);

        public static final RegistryKey<Item> GAMEBOY_CARTRIDGE = RegistryKey.of(RegistryKeys.ITEM, Id.GAMEBOY_CARTRIDGE);
        public static final RegistryKey<Item> GAMEBOY_COLOR_CARTRIDGE = RegistryKey.of(RegistryKeys.ITEM, Id.GAMEBOY_COLOR_CARTRIDGE);
        public static final RegistryKey<Item> GAMEBOY_ADVANCE_CARTRIDGE = RegistryKey.of(RegistryKeys.ITEM, Id.GAMEBOY_ADVANCE_CARTRIDGE);
        public static final RegistryKey<Item> GAME_GEAR_CARTRIDGE = RegistryKey.of(RegistryKeys.ITEM, Id.GAME_GEAR_CARTRIDGE);

        public static final RegistryKey<Item> GAMEBOY = RegistryKey.of(RegistryKeys.ITEM, Id.GAMEBOY);
        public static final RegistryKey<Item> GAMEBOY_COLOR = RegistryKey.of(RegistryKeys.ITEM, Id.GAMEBOY_COLOR);
        public static final RegistryKey<Item> GAMEBOY_ADVANCE = RegistryKey.of(RegistryKeys.ITEM, Id.GAMEBOY_ADVANCE);
        public static final RegistryKey<Item> GAME_GEAR = RegistryKey.of(RegistryKeys.ITEM, Id.GAME_GEAR);

        public static final RegistryKey<Item> MONITOR = RegistryKey.of(RegistryKeys.ITEM, BlockId.Id.MONITOR);

        public static final RegistryKey<ItemGroup> MAIN_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP, Id.MAIN_GROUP);
    }
}
