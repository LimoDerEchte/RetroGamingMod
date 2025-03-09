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

    public record ENetTokenPayload(int port, String token) implements CustomPayload {
        public static final Id<ENetTokenPayload> ID = new Id<>(NetworkId.ENET_TOKEN);
        public static final PacketCodec<RegistryByteBuf, ENetTokenPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ENetTokenPayload::port,
                PacketCodecs.STRING, ENetTokenPayload::token,
                ENetTokenPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UpdateDisplayPayload(UUID uuid, int width, int height) implements CustomPayload {
        public static final Id<UpdateDisplayPayload> ID = new Id<>(NetworkId.UPDATE_DISPLAY);
        public static final PacketCodec<RegistryByteBuf, UpdateDisplayPayload> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, UpdateDisplayPayload::uuid,
                PacketCodecs.VAR_INT, UpdateDisplayPayload::width,
                PacketCodecs.VAR_INT, UpdateDisplayPayload::height,
                UpdateDisplayPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UpdateAudioDataPayload(UUID uuid, byte[] data) implements CustomPayload {
        public static final Id<UpdateAudioDataPayload> ID = new Id<>(NetworkId.UPDATE_AUDIO_DATA);
        public static final PacketCodec<RegistryByteBuf, UpdateAudioDataPayload> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, UpdateAudioDataPayload::uuid,
                PacketCodecs.BYTE_ARRAY, UpdateAudioDataPayload::data,
                UpdateAudioDataPayload::new
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
        PayloadTypeRegistry.playS2C().register(ENetTokenPayload.ID, ENetTokenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateDisplayPayload.ID, UpdateDisplayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateAudioDataPayload.ID, UpdateAudioDataPayload.CODEC);
    }
}
