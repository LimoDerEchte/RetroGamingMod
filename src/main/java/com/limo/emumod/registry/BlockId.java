package com.limo.emumod.registry;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class BlockId {

    public static class Id {
        public static final Identifier MONITOR = Identifier.of("emumod", "monitor");
        public static final Identifier LARGE_TV = Identifier.of("emumod", "large_tv");
        public static final Identifier NES = Identifier.of("emumod", "nes");
    }

    public static class Registry {
        public static final RegistryKey<Block> MONITOR = RegistryKey.of(RegistryKeys.BLOCK, Id.MONITOR);
        public static final RegistryKey<Block> LARGE_TV = RegistryKey.of(RegistryKeys.BLOCK, Id.LARGE_TV);
        public static final RegistryKey<Block> NES = RegistryKey.of(RegistryKeys.BLOCK, Id.NES);
    }
}
