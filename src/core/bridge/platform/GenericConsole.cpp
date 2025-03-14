//
// Created by limo on 2/21/25.
//

#include "GenericConsole.hpp"

#include <iostream>
#include <headers/com_limo_emumod_bridge_NativeGenericConsole.h>
#include <jni.h>

#include "util/NativeUtil.hpp"

std::vector<GenericConsole*> GenericConsoleRegistry::consoles;
std::mutex GenericConsoleRegistry::consoleMutex;

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_init(JNIEnv *, jclass, const jlong jUuid, const jint width, const jint height) {
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    return reinterpret_cast<jlong>(new GenericConsole(width, height, uuid));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_start(JNIEnv *env, jclass, const jlong ptr, const jstring retroCore, const jstring core, const jstring rom, jstring) { // NOLINT(*-misplaced-const)
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->load(env->GetStringUTFChars(retroCore, nullptr), env->GetStringUTFChars(core, nullptr), env->GetStringUTFChars(rom, nullptr));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->dispose();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_updateInput(JNIEnv *, jclass, const jlong ptr, const jint port, const jshort input) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->input(port, input);
}

JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_getWidth(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    return gameboy->width;
}

JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_getHeight(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    return gameboy->height;
}

GenericConsole::GenericConsole(const int width, const int height, const jUUID* uuid): width(width), height(height), uuid(uuid) {
    GenerateID(id);
    GenericConsoleRegistry::registerConsole(this);
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
}

void GenericConsole::dispose() {
    std::cout << "[RetroGamingCore] Disposing bridge instance " << std::endl;
    GenericConsoleRegistry::unregisterConsole(this);
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

std::vector<uint8_t> GenericConsole::createFrame() {
    if (videoEncoder == nullptr) {
        videoEncoder = new VideoEncoderRGB565(width, height);
    }
    return videoEncoder->encode(retroCoreHandle->display);
}

void GenericConsole::input(const int port, const int16_t input) {
    std::lock_guard lock(mutex);
    if (retroCoreHandle != nullptr) {
        retroCoreHandle->controls[port] = input;
    }
}

void GenericConsoleRegistry::registerConsole(GenericConsole *console) {
    std::lock_guard guard(consoleMutex);
    consoles.emplace_back(console);
}

void GenericConsoleRegistry::unregisterConsole(GenericConsole *console) {
    std::lock_guard guard(consoleMutex);
    consoles.erase(std::ranges::remove(consoles, console).begin(), consoles.end());
}

void GenericConsoleRegistry::withConsoles(const std::function<void(GenericConsole *)>& func) {
    std::lock_guard guard(consoleMutex);
    for (const auto &console : consoles) {
        console->mutex.lock();
        func(console);
        console->mutex.unlock();
    }
}
