package com.limo.emumod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.OptionalInt;
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

    public record OpenGameScreenPayload(byte type, int streamId, OptionalInt port) implements CustomPayload {
        public static final Id<OpenGameScreenPayload> ID = new Id<>(NetworkId.OPEN_GAME_SCREEN);
        public static final PacketCodec<RegistryByteBuf, OpenGameScreenPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, OpenGameScreenPayload::type,
                PacketCodecs.VAR_INT, OpenGameScreenPayload::streamId,
                PacketCodecs.OPTIONAL_INT, OpenGameScreenPayload::port,
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

    public record EmuModTokenPayload(byte[] token) implements CustomPayload {
        public static final Id<EmuModTokenPayload> ID = new Id<>(NetworkId.ENET_TOKEN);
        public static final PacketCodec<RegistryByteBuf, EmuModTokenPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE_ARRAY, EmuModTokenPayload::token,
                EmuModTokenPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UpdateEmulatorPayload(UUID console, int id, int width, int height, int audioCodec, int videoCodec) implements CustomPayload {
        public static final Id<UpdateEmulatorPayload> ID = new Id<>(NetworkId.UPDATE_EMULATOR);
        public static final PacketCodec<RegistryByteBuf, UpdateEmulatorPayload> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, UpdateEmulatorPayload::console,
                PacketCodecs.VAR_INT, UpdateEmulatorPayload::id,
                PacketCodecs.VAR_INT, UpdateEmulatorPayload::width,
                PacketCodecs.VAR_INT, UpdateEmulatorPayload::height,
                PacketCodecs.VAR_INT, UpdateEmulatorPayload::audioCodec,
                PacketCodecs.VAR_INT, UpdateEmulatorPayload::videoCodec,
                UpdateEmulatorPayload::new
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
        PayloadTypeRegistry.playS2C().register(EmuModTokenPayload.ID, EmuModTokenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateEmulatorPayload.ID, UpdateEmulatorPayload.CODEC);
    }
}
