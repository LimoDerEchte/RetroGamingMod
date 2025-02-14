package com.limo.emumod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public class C2S {

    public record CreateCartridgePayload(byte[] data) implements CustomPayload {
        public static final Id<C2S.CreateCartridgePayload> ID = new Id<>(NetworkId.CREATE_CARTRIDGE);
        public static final PacketCodec<RegistryByteBuf, C2S.CreateCartridgePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE_ARRAY, C2S.CreateCartridgePayload::data,
                C2S.CreateCartridgePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(CreateCartridgePayload.ID, CreateCartridgePayload.CODEC);
    }
}
