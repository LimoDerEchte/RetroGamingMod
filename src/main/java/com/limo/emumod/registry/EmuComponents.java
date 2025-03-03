package com.limo.emumod.registry;

import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;

import java.util.UUID;
import java.util.function.UnaryOperator;

public class EmuComponents {
    public static final ComponentType<UUID> LINK_ID = register("link_id", builder -> builder
            .codec(Uuids.CODEC).packetCodec(Uuids.PACKET_CODEC));

    public static final ComponentType<String> GAME = register("game", builder -> builder
            .codec(Codecs.ESCAPED_STRING).packetCodec(PacketCodecs.STRING));
    public static final ComponentType<UUID> FILE_ID = register("file_id", builder -> builder
            .codec(Uuids.CODEC).packetCodec(Uuids.PACKET_CODEC));

    public static <T> ComponentType<T> register(String path, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of("emumod", path), builderOperator.apply(ComponentType.builder()).build());
    }

    public static void init() {
    }
}
