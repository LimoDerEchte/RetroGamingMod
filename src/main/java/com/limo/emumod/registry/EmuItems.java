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
import com.limo.emumod.util.AudioCodec;
import com.limo.emumod.util.VideoCodec;
import com.limo.emumod.util.FileUtil;
import com.limo.emumod.util.RequirementManager;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import java.io.File;
import java.util.UUID;

import static com.limo.emumod.network.ServerEvents.mcs;

public class EmuItems {
    public static final Item CARTRIDGE = register(new CartridgeItem(), ItemId.Registry.CARTRIDGE);
    public static final Item BROKEN_CARTRIDGE = register(new Item(new Item.Properties().stacksTo(8).setId(ItemId.Registry.BROKEN_CARTRIDGE)), ItemId.Registry.BROKEN_CARTRIDGE);

    public static final Item GAMEBOY_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_CARTRIDGE,
            "gb", () -> GenericHandheldItem.link = null, (_, file, console) ->
            runGenericConsole(RequirementManager.gearBoy, file, console, "gb", 160, 144,
                    VideoCodec.AV1, AudioCodec.Opus, 44100)), ItemId.Registry.GAMEBOY_CARTRIDGE);

    public static final Item GAMEBOY_COLOR_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_COLOR_CARTRIDGE,
            "gbc", () -> GenericHandheldItem.link = null, (_, file, console) ->
            runGenericConsole(RequirementManager.gearBoy, file, console, "gbc", 160, 144,
                    VideoCodec.AV1, AudioCodec.Opus, 44100)), ItemId.Registry.GAMEBOY_COLOR_CARTRIDGE);

    public static final Item GAMEBOY_ADVANCE_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAMEBOY_ADVANCE_CARTRIDGE,
            "gba", () -> GenericHandheldItem.link = null, (_, file, console) ->
            runGenericConsole(RequirementManager.beetleGBA, file, console, "gba", 240, 160,
                    VideoCodec.AV1, AudioCodec.Opus, 44100)), ItemId.Registry.GAMEBOY_ADVANCE_CARTRIDGE);

    public static final Item GAME_GEAR_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.GAME_GEAR_CARTRIDGE,
            "gg", () -> GenericHandheldItem.link = null, (user, file, console) ->
            runGenericConsoleWithBios(user, RequirementManager.genesisPlusGX, "bios.gg", file, console, "gg", 160, 144,
                    VideoCodec.AV1, AudioCodec.Opus, 44100)), ItemId.Registry.GAME_GEAR_CARTRIDGE);

    public static final Item GAMEBOY = register(new GenericHandheldItem(ItemId.Registry.GAMEBOY,
            NetworkId.ScreenType.GAMEBOY, GAMEBOY_CARTRIDGE), ItemId.Registry.GAMEBOY);
    public static final Item GAMEBOY_COLOR = register(new GenericHandheldItem(ItemId.Registry.GAMEBOY_COLOR,
            NetworkId.ScreenType.GAMEBOY_COLOR, GAMEBOY_COLOR_CARTRIDGE), ItemId.Registry.GAMEBOY_COLOR);
    public static final Item GAMEBOY_ADVANCE = register(new GenericHandheldItem(ItemId.Registry.GAMEBOY_ADVANCE,
            NetworkId.ScreenType.GAMEBOY_ADVANCE, GAMEBOY_ADVANCE_CARTRIDGE), ItemId.Registry.GAMEBOY_ADVANCE);
    public static final Item GAME_GEAR = register(new GenericHandheldItem(ItemId.Registry.GAME_GEAR,
            NetworkId.ScreenType.GAME_GEAR, GAME_GEAR_CARTRIDGE), ItemId.Registry.GAME_GEAR);

    public static final Item NES_CARTRIDGE = register(new LinkedCartridgeItem(ItemId.Registry.NES_CARTRIDGE), ItemId.Registry.NES_CARTRIDGE);
    public static final Item NES_CONTROLLER = register(new ControllerItem(ItemId.Registry.NES_CONTROLLER, 2), ItemId.Registry.NES_CONTROLLER);
    public static final Item NES = register(new BlockItem(EmuBlocks.NES, new Item.Properties().stacksTo(8)
            .setId(ItemId.Registry.NES)), ItemId.Registry.NES);

    public static final Item MONITOR = register(new BlockItem(EmuBlocks.MONITOR, new Item.Properties().stacksTo(8)
            .setId(ItemId.Registry.MONITOR)), ItemId.Registry.MONITOR);
    public static final Item LARGE_TV = register(new BlockItem(EmuBlocks.LARGE_TV, new Item.Properties().stacksTo(8)
            .setId(ItemId.Registry.LARGE_TV)), ItemId.Registry.LARGE_TV);
    public static final Item CABLE = register(new CableItem(), ItemId.Registry.CABLE);

    public static final CreativeModeTab MAIN_GROUP = register(FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.emumod.main"))
            .icon(GAMEBOY_ADVANCE::getDefaultInstance)
            .build(), ItemId.Registry.MAIN_GROUP);

    public static Item register(Item item, ResourceKey<Item> registryKey) {
        return Registry.register(BuiltInRegistries.ITEM, registryKey.identifier(), item);
    }

    public static CreativeModeTab register(CreativeModeTab group, ResourceKey<CreativeModeTab> registryKey) {
        return Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, registryKey.identifier(), group);
    }

    public static void init() {
        // Uninitialized Stuff
        ((LinkedCartridgeItem)GAMEBOY_CARTRIDGE).linkItem = GAMEBOY;
        ((LinkedCartridgeItem)GAMEBOY_COLOR_CARTRIDGE).linkItem = GAMEBOY_COLOR;
        ((LinkedCartridgeItem)GAMEBOY_ADVANCE_CARTRIDGE).linkItem = GAMEBOY_ADVANCE;
        ((LinkedCartridgeItem)GAME_GEAR_CARTRIDGE).linkItem = GAME_GEAR;
        // Item Group
        ItemGroupEvents.modifyEntriesEvent(ItemId.Registry.MAIN_GROUP).register(group -> {
            group.accept(CARTRIDGE);
            group.accept(BROKEN_CARTRIDGE);

            group.accept(GAMEBOY);
            group.accept(GAMEBOY_COLOR);
            group.accept(GAMEBOY_ADVANCE);
            group.accept(GAME_GEAR);

            group.accept(NES);
            group.accept(NES_CONTROLLER);

            group.accept(MONITOR);
            group.accept(LARGE_TV);
            group.accept(CABLE);
        });
    }

    public static boolean runGenericConsole(File core, UUID file, UUID console, String fileType, int width, int height,
                                            VideoCodec videoCodec, AudioCodec audioCodec, int sampleRate) {
        NativeGenericConsole con = new NativeGenericConsole(width, height, videoCodec, audioCodec, file, fileType, sampleRate);
        con.load(core);
        EmuMod.running.put(console, con);
        PlayerLookup.all(mcs).forEach(player -> ServerPlayNetworking
                .send(player, new S2C.UpdateEmulatorPayload(console, con.getId(), width, height,
                        videoCodec.ordinal(), audioCodec.ordinal(), sampleRate)));
        return true;
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean runGenericConsoleWithBios(Player user, File core, String bios, UUID file,
                                                     UUID consoleId, String fileType, int width, int height,
                                                     VideoCodec videoCodec, AudioCodec audioCodec, int sampleRate) {
        if(!FileUtil.getRequiredFile(bios).exists()) {
            user.displayClientMessage(Component.translatable("item.emumod.handheld.bios", bios), true);
            return false;
        }
        return runGenericConsole(core, file, consoleId, fileType, width, height, videoCodec, audioCodec, sampleRate);
    }
}
