
#include "NativeUtil.hpp"

#include <random>
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

long jUUID::combine() const {
    return mostSignificantBits ^ leastSignificantBits << 1;
}

void GenerateID(char* id, const int length) {
    constexpr char charset[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    constexpr int charsetSize = sizeof(charset) - 1;

    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution distrib(0, charsetSize - 1);

    for (int i = 0; i < length; ++i) {
        id[i] = charset[distrib(gen)];
    }
    id[length] = '\0';
}
