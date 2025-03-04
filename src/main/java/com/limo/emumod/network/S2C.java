package com.limo.emumod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

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

    public record OpenGameScreenPayload(byte type, UUID fileId) implements CustomPayload {
        public static final Id<OpenGameScreenPayload> ID = new Id<>(NetworkId.OPEN_GAME_SCREEN);
        public static final PacketCodec<RegistryByteBuf, OpenGameScreenPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, OpenGameScreenPayload::type,
                Uuids.PACKET_CODEC, OpenGameScreenPayload::fileId,
                OpenGameScreenPayload::new
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

    public record UpdateDisplayDataPayload(UUID uuid, byte type, byte[] data) implements CustomPayload {
        public static final Id<UpdateDisplayDataPayload> ID = new Id<>(NetworkId.UPDATE_DISPLAY_DATA);
        public static final PacketCodec<RegistryByteBuf, UpdateDisplayDataPayload> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, UpdateDisplayDataPayload::uuid,
                PacketCodecs.BYTE, UpdateDisplayDataPayload::type,
                PacketCodecs.BYTE_ARRAY, UpdateDisplayDataPayload::data,
                UpdateDisplayDataPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UpdateAudioDataPayload(UUID uuid, byte[] data) implements CustomPayload {
        public static final Id<UpdateDisplayDataPayload> ID = new Id<>(NetworkId.UPDATE_DISPLAY_DATA);
        public static final PacketCodec<RegistryByteBuf, UpdateDisplayDataPayload> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, UpdateDisplayDataPayload::uuid,
                PacketCodecs.BYTE, UpdateDisplayDataPayload::type,
                PacketCodecs.BYTE_ARRAY, UpdateDisplayDataPayload::data,
                UpdateDisplayDataPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(OpenScreenPayload.ID, OpenScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenGameScreenPayload.ID, OpenGameScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CloseScreenPayload.ID, CloseScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateDisplayDataPayload.ID, UpdateDisplayDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateAudioDataPayload.ID, UpdateAudioDataPayload.CODEC);
    }
}
