package com.limo.emumod.network;

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
            return null;
        }
    }
}
