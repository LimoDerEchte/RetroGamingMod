//
// Created by limo on 2/21/25.
//

#include "headers/com_limo_emumod_bridge_NativeDisplay.h"
#include "NativeDisplay.h"
#include <jni.h>

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeDisplay_update(JNIEnv *env, jobject obj, jobject display) {
    // Retrieve Display Object
    const auto clazz = env->GetObjectClass(display);
    const auto peer = env->GetMethodID(clazz, "peer", "()J");
    const auto addr = env->CallLongMethod(display, peer);
    const auto *nativeDisplay = reinterpret_cast<NativeDisplay *>(addr);
    // Upload updates
    if (!nativeDisplay->changed)
        return;
    const auto field = env->GetFieldID(clazz, "buf", "[I");
    const auto data = reinterpret_cast<jintArray>(env->GetObjectField(obj, field));
    env->ReleaseIntArrayElements(data, nativeDisplay->buf, 0);
}
