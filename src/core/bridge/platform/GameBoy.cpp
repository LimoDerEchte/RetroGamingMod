//
// Created by limo on 2/21/25.
//

#include "GameBoy.h"

#include <iostream>
#include <jni.h>
#include <thread>
#include <headers/com_limo_emumod_bridge_NativeGameBoy.h>
#include <mgba/core/core.h>

#include "util/VFileFix.h"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_init(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new GameBoy());
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_loadROM(JNIEnv *env, jclass, const jlong ptr, const jstring path) { // NOLINT(*-misplaced-const)
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->load(env->GetStringUTFChars(path, nullptr));
    gameboy->start();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_loadSave(JNIEnv *env, jclass, const jlong ptr, const jstring path) { // NOLINT(*-misplaced-const)
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->loadSave(env->GetStringUTFChars(path, nullptr));
    gameboy->start();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_start(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->start();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->stop();
}

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_createDisplay(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    return reinterpret_cast<jlong>(gameboy->getDisplay());
}

GameBoy::GameBoy() {
    std::lock_guard lock(mutex);
    core->init(core);
    core->setVideoBuffer(core, videoBuffer, 160 * 144);
    mCoreOptions opts = {};
    mCoreConfigMap(&core->config, &opts);
}

bool GameBoy::load(const char *path) {
    std::lock_guard lock(mutex);
    const auto rom = VFileLoadFixed(path);
    core->unloadROM(core);
    core->reset(core);
    const bool ret = core->loadROM(core, rom);
    if (!ret) {
        rom->close(rom);
    }
    return ret;
}

void GameBoy::loadSave(const char *path) {
    std::lock_guard lock(mutex);
    std::cout << path << std::endl;
}


void GameBoy::start() {
    mutex.lock();
    isRunning = true;
    std::thread(&GameBoy::mainLoop, this).detach();
    mutex.unlock();
}

void GameBoy::stop() {
    std::lock_guard lock(mutex);
    isRunning = false;
    core->deinit(core);
}

NativeDisplay *GameBoy::getDisplay() {
    if (!display) {
        display = new NativeDisplay(160, 144);
    }
    return display;
}

void GameBoy::mainLoop() {
    while (isRunning) {
        mutex.lock();
        core->runFrame(core);
        mutex.unlock();
    }
}
