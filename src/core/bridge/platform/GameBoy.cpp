//
// Created by limo on 2/21/25.
//

#include <jni.h>
#include <headers/com_limo_emumod_bridge_NativeGameBoy.h>
#include <mgba-util/vfs.h>

#include "GameBoy.h"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_init(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new GameBoy());
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_load(JNIEnv *env, jclass, const jlong ptr, const jstring *path) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->load(env->GetStringUTFChars(*path, nullptr));
    gameboy->start();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->stop();
}

void GameBoy::load(const char *path) {
    //const auto file = VFileOpen(path, 0);
    gb = new GB();
    GBCreate(gb);
    //GBLoadSave(gb, file);
}

void GameBoy::start() {
    // TODO
}

void GameBoy::stop() {
    // TODO
}
