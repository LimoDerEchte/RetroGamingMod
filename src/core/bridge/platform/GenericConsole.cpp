//
// Created by limo on 2/21/25.
//

#include "GenericConsole.hpp"

#include <iostream>
#include <headers/com_limo_emumod_bridge_NativeGenericConsole.h>
#include <jni.h>

#include "util/NativeUtil.hpp"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_init(JNIEnv *, jclass, const jint width, const jint height) {
    return reinterpret_cast<jlong>(new GenericConsole(width, height));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_start(JNIEnv *env, jclass, const jlong ptr, const jstring retroCore, const jstring core, const jstring rom, jstring) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->load(env->GetStringUTFChars(retroCore, nullptr), env->GetStringUTFChars(core, nullptr), env->GetStringUTFChars(rom, nullptr));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->dispose();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_updateInput(JNIEnv *, jclass, const jlong ptr, const jshort input) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->input(input);
}

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_createDisplay(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    return reinterpret_cast<jlong>(gameboy->getDisplay());
}

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_createAudio(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    return reinterpret_cast<jlong>(gameboy->getAudio());
}

GenericConsole::GenericConsole(const int width, const int height): width(width), height(height) {
    GenerateID(id);
}

void GenericConsole::load(const char *retroCore, const char *core, const char *rom) {
    std::lock_guard lock(mutex);
    std::string strId(id, 32);
    bip::shared_memory_object::remove(strId.c_str());
    sharedMemoryHandle = new bip::managed_shared_memory(bip::create_only, strId.c_str(), 1200000);
    retroCoreHandle = sharedMemoryHandle->construct<GenericShared>("SharedData")();
    std::cout << "[RetroGamingCore] Created shared memory " << strId << std::endl;
    retroCoreProcess = new bp::child(retroCore, bp::args({"gn", strId, core, rom}));
    retroCoreProcess->detach();
    nativeDisplay = new NativeDisplay(width, height, &retroCoreHandle->displayChanged, retroCoreHandle->display);
    nativeAudio = new NativeAudio(&retroCoreHandle->audioChanged, retroCoreHandle->audio, &retroCoreHandle->audioSize);
}

void GenericConsole::dispose() {
    std::cout << "[RetroGamingCore] Disposing bridge instance " << std::endl;
    std::lock_guard lock(mutex);
    retroCoreHandle = nullptr;
    if (retroCoreProcess != nullptr) {
        retroCoreProcess->terminate();
        std::cout << "[RetroGamingCore] Terminating emulator " << std::endl;
        retroCoreProcess->wait();
    }
    if (sharedMemoryHandle != nullptr) {
        sharedMemoryHandle->destroy<GenericShared>("SharedData");
    }
    bip::shared_memory_object::remove(id);
}

NativeDisplay *GenericConsole::getDisplay() const {
    return nativeDisplay;
}

NativeAudio *GenericConsole::getAudio() const {
    return nativeAudio;
}

void GenericConsole::input(const int16_t input) {
    std::lock_guard lock(mutex);
    if (retroCoreHandle != nullptr) {
        retroCoreHandle->controls = input;
    }
}

