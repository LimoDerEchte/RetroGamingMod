//
// Created by limo on 3/9/25.
//

#include "NativeUtil.hpp"

#include <headers/com_limo_emumod_bridge_NativeUtil.h>

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeUtil_nativeUUID(JNIEnv *, jclass, const jlong mostSignificantBits, const jlong leastSignificantBits) {
    return reinterpret_cast<jlong>(new jUUID(mostSignificantBits, leastSignificantBits));
}

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeUtil_mostSignificantBits(JNIEnv *, jclass, const jlong ptr) {
    const auto uuid = reinterpret_cast<jUUID*>(ptr);
    return uuid->mostSignificantBits;
}

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeUtil_leastSignificantBits(JNIEnv *, jclass, const jlong ptr) {
    const auto uuid = reinterpret_cast<jUUID*>(ptr);
    return uuid->leastSignificantBits;
}
