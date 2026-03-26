package com.limo.emumod.registry;

import com.limo.emumod.console.GenericConsoleBlock;
import com.limo.emumod.monitor.MonitorBlock;
import com.limo.emumod.util.AudioCodec;
import com.limo.emumod.util.VideoCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import com.limo.emumod.util.RequirementManager;

import static com.limo.emumod.registry.EmuItems.runGenericConsole;

public class EmuBlocks {
    public static final Block MONITOR = register(new MonitorBlock(), BlockId.Registry.MONITOR);
    public static final Block LARGE_TV = register(new MonitorBlock(), BlockId.Registry.LARGE_TV);

    public static final Block NES = register(new GenericConsoleBlock(BlockId.Registry.NES, EmuItems.NES_CARTRIDGE, "nes",
            (_, file, console) -> runGenericConsole(RequirementManager.FCEUmm, file, console,
                    "nes", 256, 240, VideoCodec.AV1, AudioCodec.Opus, 48000)), BlockId.Registry.NES);

    public static Block register(Block block, ResourceKey<Block> blockKey) {
        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }
}
