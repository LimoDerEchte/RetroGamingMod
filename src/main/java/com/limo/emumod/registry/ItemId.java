package com.limo.emumod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public class ItemId {

    public static class Id {
        public static final Identifier CARTRIDGE = Identifier.fromNamespaceAndPath("emumod", "cartridge");
        public static final Identifier BROKEN_CARTRIDGE = Identifier.fromNamespaceAndPath("emumod", "broken_cartridge");

        public static final Identifier GAMEBOY_CARTRIDGE = Identifier.fromNamespaceAndPath("emumod", "gameboy_cartridge");
        public static final Identifier GAMEBOY_COLOR_CARTRIDGE = Identifier.fromNamespaceAndPath("emumod", "gameboy_color_cartridge");
        public static final Identifier GAMEBOY_ADVANCE_CARTRIDGE = Identifier.fromNamespaceAndPath("emumod", "gameboy_advance_cartridge");
        public static final Identifier GAME_GEAR_CARTRIDGE = Identifier.fromNamespaceAndPath("emumod", "game_gear_cartridge");

        public static final Identifier NES_CARTRIDGE = Identifier.fromNamespaceAndPath("emumod", "nes_cartridge");

        public static final Identifier NES_CONTROLLER = Identifier.fromNamespaceAndPath("emumod", "nes_controller");

        public static final Identifier GAMEBOY = Identifier.fromNamespaceAndPath("emumod", "gameboy");
        public static final Identifier GAMEBOY_COLOR = Identifier.fromNamespaceAndPath("emumod", "gameboy_color");
        public static final Identifier GAMEBOY_ADVANCE = Identifier.fromNamespaceAndPath("emumod", "gameboy_advance");
        public static final Identifier GAME_GEAR = Identifier.fromNamespaceAndPath("emumod", "game_gear");

        public static final Identifier CABLE = Identifier.fromNamespaceAndPath("emumod", "cable");

        public static final Identifier MAIN_GROUP = Identifier.fromNamespaceAndPath("emumod", "main_group");
    }

    public static class Registry {
        public static final ResourceKey<Item> CARTRIDGE = ResourceKey.create(Registries.ITEM, Id.CARTRIDGE);
        public static final ResourceKey<Item> BROKEN_CARTRIDGE = ResourceKey.create(Registries.ITEM, Id.BROKEN_CARTRIDGE);

        public static final ResourceKey<Item> GAMEBOY_CARTRIDGE = ResourceKey.create(Registries.ITEM, Id.GAMEBOY_CARTRIDGE);
        public static final ResourceKey<Item> GAMEBOY_COLOR_CARTRIDGE = ResourceKey.create(Registries.ITEM, Id.GAMEBOY_COLOR_CARTRIDGE);
        public static final ResourceKey<Item> GAMEBOY_ADVANCE_CARTRIDGE = ResourceKey.create(Registries.ITEM, Id.GAMEBOY_ADVANCE_CARTRIDGE);
        public static final ResourceKey<Item> GAME_GEAR_CARTRIDGE = ResourceKey.create(Registries.ITEM, Id.GAME_GEAR_CARTRIDGE);

        public static final ResourceKey<Item> NES_CARTRIDGE = ResourceKey.create(Registries.ITEM, Id.NES_CARTRIDGE);

        public static final ResourceKey<Item> NES_CONTROLLER = ResourceKey.create(Registries.ITEM, Id.NES_CONTROLLER);

        public static final ResourceKey<Item> GAMEBOY = ResourceKey.create(Registries.ITEM, Id.GAMEBOY);
        public static final ResourceKey<Item> GAMEBOY_COLOR = ResourceKey.create(Registries.ITEM, Id.GAMEBOY_COLOR);
        public static final ResourceKey<Item> GAMEBOY_ADVANCE = ResourceKey.create(Registries.ITEM, Id.GAMEBOY_ADVANCE);
        public static final ResourceKey<Item> GAME_GEAR = ResourceKey.create(Registries.ITEM, Id.GAME_GEAR);

        public static final ResourceKey<Item> NES = ResourceKey.create(Registries.ITEM, BlockId.Id.NES);

        public static final ResourceKey<Item> MONITOR = ResourceKey.create(Registries.ITEM, BlockId.Id.MONITOR);
        public static final ResourceKey<Item> LARGE_TV = ResourceKey.create(Registries.ITEM, BlockId.Id.LARGE_TV);
        public static final ResourceKey<Item> CABLE = ResourceKey.create(Registries.ITEM, Id.CABLE);

        public static final ResourceKey<CreativeModeTab> MAIN_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Id.MAIN_GROUP);
    }
}
