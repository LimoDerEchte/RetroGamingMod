package com.limo.emumod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public class C2S {

    public record CreateCartridgePayload(int handle, byte cartType, byte[] data) implements CustomPacketPayload {
        public static final Type<CreateCartridgePayload> ID = new Type<>(NetworkId.CREATE_CARTRIDGE);
        public static final StreamCodec<RegistryFriendlyByteBuf, CreateCartridgePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, CreateCartridgePayload::handle,
                ByteBufCodecs.BYTE, CreateCartridgePayload::cartType,
                ByteBufCodecs.BYTE_ARRAY, CreateCartridgePayload::data,
                CreateCartridgePayload::new
        );

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(CreateCartridgePayload.ID, CreateCartridgePayload.CODEC);
    }
}
