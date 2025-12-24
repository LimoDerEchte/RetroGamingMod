package com.limo.emumod.registry;

import com.limo.emumod.EmuMod;
import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.cartridge.CartridgeItem;
import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.console.ControllerItem;
import com.limo.emumod.console.GenericHandheldItem;
import com.limo.emumod.monitor.CableItem;
import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import com.limo.emumod.util.Codec;
import com.limo.emumod.util.FileUtil;
import com.limo.emumod.util.RequirementManager;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;

import java.io.File;
import java.util.UUID;

import static com.limo.emumod.network.ServerEvents.mcs;

public class EmuItems {
    public static final Item CARTRIDGE = register(new CartridgeItem(), ItemId.Registry.CARTRIDGE);
    public static final Item BROKEN_CARTRIDGE = register(new Item(new Item.Settings().maxCount(8).registryKey(ItemId.Registry.BROKEN_CARTRIDGE)), ItemId.Registry.BROKEN_CARTRIDGE);

    public static final Item GAMEBOY_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_CARTRIDGE,
            "gb", () -> GenericHandheldItem.link = null, (user, file) ->
            runGenericConsole(RequirementManager.gearBoy, file, "gb", 160, 144,
                    44100, Codec.CODEC_WEBP)), ItemId.Registry.GAMEBOY_CARTRIDGE);

    public static final Item GAMEBOY_COLOR_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_COLOR_CARTRIDGE,
            "gbc", () -> GenericHandheldItem.link = null, (user, file) ->
            runGenericConsole(RequirementManager.gearBoy, file, "gbc", 160, 144,
                    44100, Codec.CODEC_WEBP)), ItemId.Registry.GAMEBOY_COLOR_CARTRIDGE);

    public static final Item GAMEBOY_ADVANCE_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_ADVANCE_CARTRIDGE,
            "gba", () -> GenericHandheldItem.link = null, (user, file) ->
            runGenericConsole(RequirementManager.beetleGBA, file, "gba", 240, 160,
                    44100, Codec.CODEC_WEBP)), ItemId.Registry.GAMEBOY_ADVANCE_CARTRIDGE);

    public static final Item GAME_GEAR_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAME_GEAR_CARTRIDGE,
            "gg", () -> GenericHandheldItem.link = null, (user, file) ->
            runGenericConsoleWithBios(user, RequirementManager.genesisPlusGX, "bios.gg", file, "gg", 160, 144,
                    44100, Codec.CODEC_WEBP)), ItemId.Registry.GAME_GEAR_CARTRIDGE);

    public static final Item GAMEBOY = register(new GenericHandheldItem(ItemId.Registry.GAMEBOY,
            NetworkId.ScreenType.GAMEBOY, GAMEBOY_CARTRIDGE), ItemId.Registry.GAMEBOY);
    public static final Item GAMEBOY_COLOR = register(new GenericHandheldItem(ItemId.Registry.GAMEBOY_COLOR,
            NetworkId.ScreenType.GAMEBOY_COLOR, GAMEBOY_COLOR_CARTRIDGE), ItemId.Registry.GAMEBOY_COLOR);
    public static final Item GAMEBOY_ADVANCE = register(new GenericHandheldItem(ItemId.Registry.GAMEBOY_ADVANCE,
            NetworkId.ScreenType.GAMEBOY_ADVANCE, GAMEBOY_ADVANCE_CARTRIDGE), ItemId.Registry.GAMEBOY_ADVANCE);
    public static final Item GAME_GEAR = register(new GenericHandheldItem(ItemId.Registry.GAME_GEAR,
            NetworkId.ScreenType.GAME_GEAR, GAME_GEAR_CARTRIDGE), ItemId.Registry.GAME_GEAR);

    public static final Item NES = register(new BlockItem(EmuBlocks.NES, new Item.Settings().maxCount(8)
            .registryKey(ItemId.Registry.NES)), ItemId.Registry.NES);
    public static final Item NES_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.NES_CARTRIDGE), ItemId.Registry.NES_CARTRIDGE);
    public static final Item NES_CONTROLLER = register(new ControllerItem(ItemId.Registry.NES_CONTROLLER, 2), ItemId.Registry.NES_CONTROLLER);

    public static final Item MONITOR = register(new BlockItem(EmuBlocks.MONITOR, new Item.Settings().maxCount(8)
            .registryKey(ItemId.Registry.MONITOR)), ItemId.Registry.MONITOR);
    public static final Item LARGE_TV = register(new BlockItem(EmuBlocks.LARGE_TV, new Item.Settings().maxCount(8)
            .registryKey(ItemId.Registry.LARGE_TV)), ItemId.Registry.LARGE_TV);
    public static final Item CABLE = register(new CableItem(), ItemId.Registry.CABLE);

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
        // Uninitialized Stuff
        ((LinkedCartridgeItem)GAMEBOY_CARTRIDGE).linkItem = GAMEBOY;
        ((LinkedCartridgeItem)GAMEBOY_COLOR_CARTRIDGE).linkItem = GAMEBOY_COLOR;
        ((LinkedCartridgeItem)GAMEBOY_ADVANCE_CARTRIDGE).linkItem = GAMEBOY_ADVANCE;
        ((LinkedCartridgeItem)GAME_GEAR_CARTRIDGE).linkItem = GAME_GEAR;
        // Item Group
        ItemGroupEvents.modifyEntriesEvent(ItemId.Registry.MAIN_GROUP).register(group -> {
            group.add(CARTRIDGE);
            group.add(BROKEN_CARTRIDGE);

            group.add(GAMEBOY);
            group.add(GAMEBOY_COLOR);
            group.add(GAMEBOY_ADVANCE);
            group.add(GAME_GEAR);

            group.add(NES);
            group.add(NES_CONTROLLER);

            group.add(MONITOR);
            group.add(LARGE_TV);
            group.add(CABLE);
        });
    }

    public static boolean runGenericConsole(File core, UUID file, String fileType, int width, int height, int sampleRate, int codec) {
        NativeGenericConsole con = new NativeGenericConsole(width, height, sampleRate, codec, file, fileType);
        con.load(core);
        EmuMod.running.put(file, con);
        PlayerLookup.all(mcs).forEach(player ->
                ServerPlayNetworking.send(player, new S2C.UpdateEmulatorPayload(file, width, height, sampleRate, codec)));
        return true;
    }

    private static boolean runGenericConsoleWithBios(PlayerEntity user, File core, String bios, UUID file, String fileType, int width, int height, int sampleRate, int codec) {
        if(!FileUtil.getRequiredFile(bios).exists()) {
            user.sendMessage(Text.translatable("item.emumod.handheld.bios", bios), true);
            return false;
        }
        return runGenericConsole(core, file, fileType, width, height, sampleRate, codec);
    }
}
