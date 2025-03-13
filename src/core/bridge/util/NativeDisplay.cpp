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
    return nativeDisplay->changed;
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeDisplay_update(JNIEnv *env, const jobject obj, const jlong display) {
    const auto nativeDisplay = reinterpret_cast<NativeDisplay*>(display);
    std::lock_guard lock(nativeDisplay->mutex);
    if (!nativeDisplay->changed)
        return;
    nativeDisplay->changed = false;
    const auto clazz = env->GetObjectClass(obj);
    const auto field = env->GetFieldID(clazz, "buf", "[I");
    const auto data = reinterpret_cast<jintArray>(env->GetObjectField(obj, field));
    env->SetIntArrayRegion(data, 0, static_cast<jsize>(nativeDisplay->bufSize), reinterpret_cast<jint*>(nativeDisplay->buf));
}

NativeDisplay::NativeDisplay(const int width, const int height) : width(width), height(height) {
    std::lock_guard lock(mutex);
    bufSize = width * height;
    buf = new uint32_t[bufSize];
    changed = new bool;
}

void NativeDisplay::receive(const uint8_t *data, const size_t size) {
    std::lock_guard lock(mutex);
    if (decoder == nullptr) {
        decoder = new VideoDecoderARGB(width, height);
    }
    decoder->decode(std::vector(data, data + size), buf);
    changed = true;
}
