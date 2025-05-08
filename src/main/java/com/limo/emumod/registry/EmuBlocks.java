package com.limo.emumod.registry;

import com.limo.emumod.EmuMod;
import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.console.GenericConsoleBlock;
import com.limo.emumod.monitor.MonitorBlock;
import com.limo.emumod.network.S2C;
import com.limo.emumod.util.RequirementManager;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.io.File;
import java.util.UUID;

import static com.limo.emumod.network.ServerEvents.mcs;

public class EmuBlocks {
    public static final Block MONITOR = register(new MonitorBlock(), BlockId.Registry.MONITOR);
    public static final Block LARGE_TV = register(new MonitorBlock(), BlockId.Registry.LARGE_TV);

    public static final Block NES = register(new GenericConsoleBlock(BlockId.Registry.NES, EmuItems.NES_CARTRIDGE, "nes",
            (user, file) -> runGenericConsole(RequirementManager.FCEUmm, file, "nes", 256, 240, 48000)), BlockId.Registry.NES);

    public static Block register(Block block, RegistryKey<Block> blockKey) {
        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static boolean runGenericConsole(File core, UUID file, String fileType, int width, int height, int sampleRate) {
        NativeGenericConsole con = new NativeGenericConsole(width, height, sampleRate, file, fileType);
        con.load(core);
        EmuMod.running.put(file, con);
        PlayerLookup.all(mcs).forEach(player ->
                ServerPlayNetworking.send(player, new S2C.UpdateEmulatorPayload(file, width, height, sampleRate)));
        return true;
    }
}
