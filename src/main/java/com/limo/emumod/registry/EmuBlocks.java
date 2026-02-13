package com.limo.emumod.registry;

import com.limo.emumod.console.GenericConsoleBlock;
import com.limo.emumod.monitor.MonitorBlock;
import com.limo.emumod.util.VideoCodec;
import com.limo.emumod.util.RequirementManager;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import static com.limo.emumod.registry.EmuItems.runGenericConsole;

public class EmuBlocks {
    public static final Block MONITOR = register(new MonitorBlock(), BlockId.Registry.MONITOR);
    public static final Block LARGE_TV = register(new MonitorBlock(), BlockId.Registry.LARGE_TV);

    public static final Block NES = register(new GenericConsoleBlock(BlockId.Registry.NES, EmuItems.NES_CARTRIDGE, "nes",
            (user, file, console) -> runGenericConsole(RequirementManager.FCEUmm, file, console,
                    "nes", 256, 240, 48000, VideoCodec.AV1)), BlockId.Registry.NES);

    public static Block register(Block block, RegistryKey<Block> blockKey) {
        return Registry.register(Registries.BLOCK, blockKey, block);
    }
}
