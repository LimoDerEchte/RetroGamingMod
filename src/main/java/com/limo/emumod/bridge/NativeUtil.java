package com.limo.emumod.bridge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NativeUtil {
    private static final Map<UUID, Long> knownUuids = new HashMap<>();

    public static long nativeUUID(UUID uuid) {
        if(!knownUuids.containsKey(uuid))
            knownUuids.put(uuid, nativeUUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
        return knownUuids.get(uuid);
    }

    public static UUID nativeUUID(long uuid) {
        return new UUID(mostSignificantBits(uuid), leastSignificantBits(uuid));
    }

    private static native long nativeUUID(long mostSignificantBits, long leastSignificantBits);
    private static native long mostSignificantBits(long uuid);
    private static native long leastSignificantBits(long uuid);
}
