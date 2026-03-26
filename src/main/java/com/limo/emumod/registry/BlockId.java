package com.limo.emumod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public class BlockId {

    public static class Id {
        public static final Identifier MONITOR = Identifier.fromNamespaceAndPath("emumod", "monitor");
        public static final Identifier LARGE_TV = Identifier.fromNamespaceAndPath("emumod", "large_tv");
        public static final Identifier NES = Identifier.fromNamespaceAndPath("emumod", "nes");
    }

    public static class Registry {
        public static final ResourceKey<Block> MONITOR = ResourceKey.create(Registries.BLOCK, Id.MONITOR);
        public static final ResourceKey<Block> LARGE_TV = ResourceKey.create(Registries.BLOCK, Id.LARGE_TV);
        public static final ResourceKey<Block> NES = ResourceKey.create(Registries.BLOCK, Id.NES);
    }
}
