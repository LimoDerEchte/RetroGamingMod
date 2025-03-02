//
// Created by limo on 2/21/25.
//

#include "GameBoy.hpp"

#include <iostream>
#include <headers/com_limo_emumod_bridge_NativeGameBoy.h>
#include <jni.h>
#include <bits/std_thread.h>

#include "sys/paths.h"
#include "util/util.hpp"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_init(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new GameBoy());
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_start(JNIEnv *env, jclass, const jlong ptr, const jstring rom, jstring) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->load(env->GetStringUTFChars(rom, nullptr));
    gameboy->start();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->dispose();
}

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_createDisplay(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    return reinterpret_cast<jlong>(gameboy->getDisplay());
}

void GameBoy::load(const char *rom) {
    std::lock_guard lock(mutex);
    libRetroCore = new LibRetroCore(GB_CORE_PATH);
    if (!libRetroCore->loadCore()) {
        std::cerr << "[RetroGamingCore] Failed to load core library" << std::endl;
        return;
    }
    if (!libRetroCore->loadROM(rom)) {
        std::cerr << "[RetroGamingCore] Failed to load ROM" << std::endl;
        return;
    }
    libRetroCore->setVideoFrameCallback([this](const int* data, const unsigned width, const unsigned height, const size_t pitch) {
        std::lock_guard lLock(mutex);
        std::lock_guard dLock(nativeDisplay->mutex);
        const auto pixelData = reinterpret_cast<const uint8_t*>(data);
        for (unsigned y = 0; y < height; ++y) {
            for (unsigned x = 0; x < width; ++x) {
                const uint8_t* pixel = pixelData + y * pitch + x * 2;
                const uint16_t rgb565 = pixel[0] | pixel[1] << 8;
                const uint8_t r = ((rgb565 >> 11) & 0x1F) << 3;
                const uint8_t g = ((rgb565 >> 5) & 0x3F) << 2;
                const uint8_t b = (rgb565 & 0x1F) << 3;
                constexpr uint8_t a = 0xFF;
                nativeDisplay->buf[y * width + x] = a << 24 | r << 16 | g << 8 | b;
                nativeDisplay->changed = true;
            }
        }
    });
}

void GameBoy::start() {
    std::cout << "[RetroGamingCore] Starting up new bridge instance" << std::endl;
    std::lock_guard lock(mutex);
    std::thread t([this] {
        libRetroCore->runCore();
    });
    t.detach();
}

void GameBoy::dispose() {
    std::cout << "[RetroGamingCore] Disposing bridge instance " << std::endl;
    std::lock_guard lock(mutex);
    if (libRetroCore)
        libRetroCore->dispose();
    libRetroCore = nullptr;
}

NativeDisplay *GameBoy::getDisplay() const {
    return nativeDisplay;
}

