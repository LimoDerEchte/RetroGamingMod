package com.limo.emumod.registry;

import com.limo.emumod.console.GenericConsoleBlockEntity;
import com.limo.emumod.monitor.MonitorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class EmuBlockEntities {
    public static final BlockEntityType<MonitorBlockEntity> MONITOR = register("monitor",
            FabricBlockEntityTypeBuilder.create(MonitorBlockEntity::new, EmuBlocks.MONITOR, EmuBlocks.LARGE_TV).build());

    public static final BlockEntityType<GenericConsoleBlockEntity> GENERIC_CONSOLE = register("console",
            FabricBlockEntityTypeBuilder.create(GenericConsoleBlockEntity::new, EmuBlocks.NES).build());

    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("emumod", path), blockEntityType);
    }

    public static void init() {
    }
}
