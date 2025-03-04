//
// Created by limo on 2/21/25.
//

#include "GameBoy.hpp"

#include <iostream>
#include <headers/com_limo_emumod_bridge_NativeGameBoy.h>
#include <jni.h>

#include "util/util.hpp"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_init(JNIEnv *, jclass, const jboolean isGBA) {
    return reinterpret_cast<jlong>(new GameBoy(isGBA));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_start(JNIEnv *env, jclass, const jlong ptr, const jstring retroCore, const jstring core, const jstring rom, jstring) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->load(env->GetStringUTFChars(retroCore, nullptr), env->GetStringUTFChars(core, nullptr), env->GetStringUTFChars(rom, nullptr));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->dispose();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_updateInput(JNIEnv *, jclass, const jlong ptr, const jshort input) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->input(input);
}

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_createDisplay(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    return reinterpret_cast<jlong>(gameboy->getDisplay());
}

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_createAudio(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    return reinterpret_cast<jlong>(gameboy->getAudio());
}

GameBoy::GameBoy(const bool isGBA): isGba(isGBA) {
    GenerateID(id);
}

void GameBoy::load(const char *retroCore, const char *core, const char *rom) {
    std::lock_guard lock(mutex);
    std::string strId(id, 32);
    bip::shared_memory_object::remove(strId.c_str());
    sharedMemoryHandle = new bip::managed_shared_memory(bip::create_only, strId.c_str(), 262144);
    retroCoreHandle = sharedMemoryHandle->construct<GameBoyShared>("SharedData")();
    std::cout << "[RetroGamingCore] Created shared memory " << strId << std::endl;
    retroCoreProcess = new bp::child(retroCore, bp::args({"gb", strId, core, rom}));
    retroCoreProcess->detach();
    nativeDisplay = new NativeDisplay(isGba ? 240 : 160, isGba ? 160 : 144,
        &retroCoreHandle->displayChanged, retroCoreHandle->display);
    nativeAudio = new NativeAudio(&retroCoreHandle->audioChanged, retroCoreHandle->audio, &retroCoreHandle->audioSize);
}

void GameBoy::dispose() {
    std::cout << "[RetroGamingCore] Disposing bridge instance " << std::endl;
    std::lock_guard lock(mutex);
    retroCoreHandle = nullptr;
    if (retroCoreProcess != nullptr) {
        retroCoreProcess->terminate();
        std::cout << "[RetroGamingCore] Terminating emulator " << std::endl;
        retroCoreProcess->wait();
    }
    if (sharedMemoryHandle != nullptr) {
        sharedMemoryHandle->destroy<GameBoyShared>("SharedData");
    }
    bip::shared_memory_object::remove(id);
}

NativeDisplay *GameBoy::getDisplay() const {
    return nativeDisplay;
}

NativeAudio *GameBoy::getAudio() const {
    return nativeAudio;
}

void GameBoy::input(const int16_t input) {
    std::lock_guard lock(mutex);
    if (retroCoreHandle != nullptr) {
        retroCoreHandle->controls = input;
    }
}

