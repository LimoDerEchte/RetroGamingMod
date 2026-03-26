package com.limo.emumod.registry;

import com.limo.emumod.components.ConsoleComponent;
import com.limo.emumod.components.GameComponent;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.item.v1.ComponentTooltipAppenderRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class EmuComponents {
    public static final DataComponentType<UUID> CONSOLE_LINK_ID = register("link_id", builder -> builder
            .persistent(UUIDUtil.AUTHLIB_CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DataComponentType<Integer> PORT_NUM = register("port_num", builder -> builder
            .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DataComponentType<GameComponent> GAME = register("game", builder -> builder
            .persistent(GameComponent.CODEC).networkSynchronized(GameComponent.PACKET_CODEC));
    public static final DataComponentType<ConsoleComponent> CONSOLE = register("console", builder -> builder
            .persistent(ConsoleComponent.CODEC).networkSynchronized(ConsoleComponent.PACKET_CODEC));
    public static final DataComponentType<ItemContainerContents> CARTRIDGE = register("cartridge", builder -> builder
            .persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    public static <T> DataComponentType<T> register(String path, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath("emumod", path), builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void init() {
        ComponentTooltipAppenderRegistry.addFirst(GAME);
        ComponentTooltipAppenderRegistry.addFirst(CONSOLE);
    }
}