//
// Created by limo on 2/21/25.
//

#include "NativeDisplay.hpp"

#include <headers/com_limo_emumod_bridge_NativeDisplay.h>
#include <jni.h>

JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeDisplay_bufSize(JNIEnv *, jclass, const jlong display) {
    const auto nativeDisplay = reinterpret_cast<NativeDisplay*>(display);
    std::lock_guard lock(nativeDisplay->mutex);
    return static_cast<jint>(nativeDisplay->bufSize);
}

JNIEXPORT jboolean JNICALL Java_com_limo_emumod_bridge_NativeDisplay_hasChanged(JNIEnv *, jclass, const jlong display) {
    const auto nativeDisplay = reinterpret_cast<NativeDisplay*>(display);
    std::lock_guard lock(nativeDisplay->mutex);
    return *nativeDisplay->changed;
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeDisplay_update(JNIEnv *env, const jobject obj, const jlong display) {
    const auto nativeDisplay = reinterpret_cast<NativeDisplay*>(display);
    std::lock_guard lock(nativeDisplay->mutex);
    if (!nativeDisplay->changed)
        return;
    [](bool &ref) {
        ref = false;
    }(*nativeDisplay->changed);
    jint jDisplay[nativeDisplay->bufSize];
    for (unsigned y = 0; y < nativeDisplay->height; ++y) {
        for (unsigned x = 0; x < nativeDisplay->width; ++x) {
            const auto rgb565 = nativeDisplay->buf[y * nativeDisplay->width + x];
            const uint8_t r = (rgb565 >> 11 & 0x1F) << 3;
            const uint8_t g = (rgb565 >> 5 & 0x3F) << 2;
            const uint8_t b = (rgb565 & 0x1F) << 3;
            constexpr uint8_t a = 0xFF;
            jDisplay[y * nativeDisplay->width + x] = a << 24 | r << 16 | g << 8 | b;
        }
    }
    const auto clazz = env->GetObjectClass(obj);
    const auto field = env->GetFieldID(clazz, "buf", "[I");
    const auto data = reinterpret_cast<jintArray>(env->GetObjectField(obj, field));
    env->SetIntArrayRegion(data, 0, static_cast<jsize>(nativeDisplay->bufSize), jDisplay);
}
