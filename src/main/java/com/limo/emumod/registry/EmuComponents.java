package com.limo.emumod.registry;

import com.limo.emumod.components.ConsoleComponent;
import com.limo.emumod.components.GameComponent;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.item.v1.ComponentTooltipAppenderRegistry;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;
import java.util.function.UnaryOperator;

public class EmuComponents {
    public static final ComponentType<UUID> CONSOLE_LINK_ID = register("link_id", builder -> builder
            .codec(Uuids.CODEC).packetCodec(Uuids.PACKET_CODEC));
    public static final ComponentType<Integer> PORT_NUM = register("port_num", builder -> builder
            .codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT));

    public static final ComponentType<GameComponent> GAME = register("game", builder -> builder
            .codec(GameComponent.CODEC).packetCodec(GameComponent.PACKET_CODEC));
    public static final ComponentType<ConsoleComponent> CONSOLE = register("console", builder -> builder
            .codec(ConsoleComponent.CODEC).packetCodec(ConsoleComponent.PACKET_CODEC));
    public static final ComponentType<ContainerComponent> CARTRIDGE = register("cartridge", builder -> builder
            .codec(ContainerComponent.CODEC).packetCodec(ContainerComponent.PACKET_CODEC));

    public static <T> ComponentType<T> register(String path, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of("emumod", path), builderOperator.apply(ComponentType.builder()).build());
    }

    public static void init() {
        ComponentTooltipAppenderRegistry.addFirst(GAME);
        ComponentTooltipAppenderRegistry.addFirst(CONSOLE);
    }
}