package com.limo.emumod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.OptionalInt;
import java.util.UUID;

public class S2C {

    public record OpenScreenPayload(byte screenType) implements CustomPacketPayload {
        public static final Type<OpenScreenPayload> ID = new Type<>(NetworkId.OPEN_SCREEN);
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreenPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE, OpenScreenPayload::screenType,
                OpenScreenPayload::new
        );

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record OpenGameScreenPayload(byte screenType, int streamId, OptionalInt port) implements CustomPacketPayload {
        public static final Type<OpenGameScreenPayload> ID = new Type<>(NetworkId.OPEN_GAME_SCREEN);
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenGameScreenPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE, OpenGameScreenPayload::screenType,
                ByteBufCodecs.VAR_INT, OpenGameScreenPayload::streamId,
                ByteBufCodecs.OPTIONAL_VAR_INT, OpenGameScreenPayload::port,
                OpenGameScreenPayload::new
        );

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record CloseScreenPayload(int handle) implements CustomPacketPayload {
        public static final Type<CloseScreenPayload> ID = new Type<>(NetworkId.CLOSE_SCREEN);
        public static final StreamCodec<RegistryFriendlyByteBuf, CloseScreenPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, CloseScreenPayload::handle,
                CloseScreenPayload::new
        );

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record EmuModTokenPayload(byte[] token) implements CustomPacketPayload {
        public static final Type<EmuModTokenPayload> ID = new Type<>(NetworkId.ENET_TOKEN);
        public static final StreamCodec<RegistryFriendlyByteBuf, EmuModTokenPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE_ARRAY, EmuModTokenPayload::token,
                EmuModTokenPayload::new
        );

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record UpdateEmulatorPayload(UUID console, int id, int width, int height, int audioCodec, int videoCodec, int sampleRate) implements CustomPacketPayload {
        public static final Type<UpdateEmulatorPayload> ID = new Type<>(NetworkId.UPDATE_EMULATOR);
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateEmulatorPayload> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, UpdateEmulatorPayload::console,
                ByteBufCodecs.VAR_INT, UpdateEmulatorPayload::id,
                ByteBufCodecs.VAR_INT, UpdateEmulatorPayload::width,
                ByteBufCodecs.VAR_INT, UpdateEmulatorPayload::height,
                ByteBufCodecs.VAR_INT, UpdateEmulatorPayload::audioCodec,
                ByteBufCodecs.VAR_INT, UpdateEmulatorPayload::videoCodec,
                ByteBufCodecs.VAR_INT, UpdateEmulatorPayload::sampleRate,
                UpdateEmulatorPayload::new
        );

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
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
