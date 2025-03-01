//
// Created by limo on 2/21/25.
//

#include <MainStructures.hpp>
#include <headers/com_limo_emumod_bridge_NativeDisplay.h>
#include <jni.h>

struct NativeDisplay;
JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeDisplay_bufSize(JNIEnv *, jclass, const jlong display) {
    const auto nativeDisplay = reinterpret_cast<NativeDisplay*>(display);
    return static_cast<jint>(nativeDisplay->bufSize);
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeDisplay_update(JNIEnv *env, const jobject *obj, const jlong display) {
    const auto nativeDisplay = reinterpret_cast<NativeDisplay*>(display);
    if (!nativeDisplay->changed)
        return;
    const auto clazz = env->GetObjectClass(*obj);
    const auto field = env->GetFieldID(clazz, "buf", "[I");
    const auto data = reinterpret_cast<jintArray>(env->GetObjectField(*obj, field));
    env->ReleaseIntArrayElements(data, nativeDisplay->buf, 0);
}
