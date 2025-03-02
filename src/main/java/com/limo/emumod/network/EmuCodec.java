package com.limo.emumod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;

public class EmuCodec {

    public static final PacketCodec<ByteBuf, int[]> INT_ARRAY = new PacketCodec<>() {
        public int[] decode(ByteBuf buf) {
            int length = buf.readInt();
            int[] array = new int[length];
            for (int i = 0; i < length; i++) {
                array[i] = buf.readInt();
            }
            return array;
        }

        public void encode(ByteBuf buf, int[] value) {
            buf.writeInt(value.length);
            for (int val : value) {
                buf.writeInt(val);
            }
        }
    };
}
