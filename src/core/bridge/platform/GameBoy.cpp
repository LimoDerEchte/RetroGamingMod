//
// Created by limo on 2/21/25.
//

#include <jni.h>
#include <headers/com_limo_emumod_bridge_NativeGameBoy.h>
#include "GameBoy.h"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_init(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new GameBoy());
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_load(JNIEnv *env, jclass, const jlong ptr, const jstring *path) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    const auto cPath = std::pmr::string(env->GetStringUTFChars(*path, nullptr));
    gameboy->load(cPath);
    gameboy->start();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->stop();
}

void GameBoy::load(const std::pmr::string path) {

}

void GameBoy::start() {

}

void GameBoy::stop() {

}
