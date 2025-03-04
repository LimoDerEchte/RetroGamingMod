//
// Created by limo on 3/4/25.
//

#include "NativeAudio.hpp"

#include <headers/com_limo_emumod_bridge_NativeAudio.h>

JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeAudio_bufSize(JNIEnv *, jclass, const jlong audio) {
    const auto nativeAudio = reinterpret_cast<NativeAudio*>(audio);
    std::lock_guard lock(nativeAudio->mutex);
    return static_cast<jint>(nativeAudio->bufSize);
}

JNIEXPORT jboolean JNICALL Java_com_limo_emumod_bridge_NativeAudio_hasChanged(JNIEnv *, jclass, const jlong display) {
    const auto nativeAudio = reinterpret_cast<NativeAudio*>(display);
    std::lock_guard lock(nativeAudio->mutex);
    return *nativeAudio->changed;
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeAudio_update(JNIEnv *env, const jobject obj, const jlong audio) {
    const auto nativeAudio = reinterpret_cast<NativeAudio*>(audio);
    std::lock_guard lock(nativeAudio->mutex);
    if (!nativeAudio->changed)
        return;
    [](bool &ref) {
        ref = false;
    }(*nativeAudio->changed);
    const auto clazz = env->GetObjectClass(obj);
    const auto field = env->GetFieldID(clazz, "buf", "[S");
    const auto data = reinterpret_cast<jshortArray>(env->GetObjectField(obj, field));
    env->SetShortArrayRegion(data, 0, static_cast<jsize>(nativeAudio->bufSize), nativeAudio->buf);
}
