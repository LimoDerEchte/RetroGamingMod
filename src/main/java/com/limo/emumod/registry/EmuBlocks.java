package com.limo.emumod.registry;

import com.limo.emumod.monitor.MonitorBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public class EmuBlocks {
    public static final Block MONITOR = register(new MonitorBlock(), BlockId.Registry.MONITOR);
    public static final Block LARGE_TV = register(new MonitorBlock(), BlockId.Registry.LARGE_TV);

    public static Block register(Block block, RegistryKey<Block> blockKey) {
        return Registry.register(Registries.BLOCK, blockKey, block);
    }
}
