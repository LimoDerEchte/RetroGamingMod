package com.limo.emumod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public class C2S {

    public record CreateCartridgePayload(int handle, byte[] data) implements CustomPayload {
        public static final Id<CreateCartridgePayload> ID = new Id<>(NetworkId.CREATE_CARTRIDGE);
        public static final PacketCodec<RegistryByteBuf, CreateCartridgePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, CreateCartridgePayload::handle,
                PacketCodecs.BYTE_ARRAY, CreateCartridgePayload::data,
                CreateCartridgePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UpdateGameControls(UUID uuid, short input) implements CustomPayload {
        public static final Id<UpdateGameControls> ID = new Id<>(NetworkId.UPDATE_CONTROLS);
        public static final PacketCodec<RegistryByteBuf, UpdateGameControls> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, UpdateGameControls::uuid,
                PacketCodecs.SHORT, UpdateGameControls::input,
                UpdateGameControls::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(CreateCartridgePayload.ID, CreateCartridgePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateGameControls.ID, UpdateGameControls.CODEC);
    }
}
