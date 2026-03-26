package com.limo.emumod.registry;

import com.limo.emumod.console.GenericConsoleBlockEntity;
import com.limo.emumod.monitor.MonitorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class EmuBlockEntities {
    public static final BlockEntityType<MonitorBlockEntity> MONITOR = register("monitor",
            FabricBlockEntityTypeBuilder.create(MonitorBlockEntity::new, EmuBlocks.MONITOR, EmuBlocks.LARGE_TV).build());

    public static final BlockEntityType<GenericConsoleBlockEntity> GENERIC_CONSOLE = register("console",
            FabricBlockEntityTypeBuilder.create(GenericConsoleBlockEntity::new, EmuBlocks.NES).build());

    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath("emumod", path), blockEntityType);
    }

    public static void init() {
    }
}
