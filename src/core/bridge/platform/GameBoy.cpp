//
// Created by limo on 2/21/25.
//

#include "GameBoy.hpp"

#include <headers/com_limo_emumod_bridge_NativeGameBoy.h>
#include <jni.h>

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_init(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new GameBoy());
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_load(JNIEnv *env, jclass, const jlong ptr, const jstring path) { // NOLINT(*-misplaced-const)
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->allocate(env->GetStringUTFChars(path, nullptr));
    gameboy->start();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->dispose();
}

GameBoy::GameBoy() {
    id = std::rand()
}

void GameBoy::allocate(const char *rom) {
}

void GameBoy::start() {
}

void GameBoy::dispose() {
}

