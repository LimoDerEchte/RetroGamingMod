package com.limo.emumod.bridge;

import java.util.UUID;

public class NativeUtil {

    public static long nativeUUID(UUID uuid) {
        return nativeUUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    public static UUID nativeUUID(long uuid) {
        return new UUID(mostSignificantBits(uuid), leastSignificantBits(uuid));
    }

    private static native long nativeUUID(long mostSignificantBits, long leastSignificantBits);
    private static native long mostSignificantBits(long uuid);
    private static native long leastSignificantBits(long uuid);
}
