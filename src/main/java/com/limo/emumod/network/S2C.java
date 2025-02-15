package com.limo.emumod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public class S2C {

    public record OpenScreenPayload(byte type) implements CustomPayload {
        public static final Id<OpenScreenPayload> ID = new Id<>(NetworkId.OPEN_SCREEN);
        public static final PacketCodec<RegistryByteBuf, OpenScreenPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, OpenScreenPayload::type,
                OpenScreenPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record CloseScreenPayload(int handle) implements CustomPayload {
        public static final Id<CloseScreenPayload> ID = new Id<>(NetworkId.CLOSE_SCREEN);
        public static final PacketCodec<RegistryByteBuf, CloseScreenPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, CloseScreenPayload::handle,
                CloseScreenPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CloseScreenPayload.ID, CloseScreenPayload.CODEC);
    }
}
