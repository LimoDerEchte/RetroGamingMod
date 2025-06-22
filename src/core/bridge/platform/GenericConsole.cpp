//
// Created by limo on 2/21/25.
//

#include "GenericConsole.hpp"

#include <iostream>
#include <headers/com_limo_emumod_bridge_NativeGenericConsole.h>
#include <jni.h>
#include <thread>

#include "codec/AudioEncoder.hpp"
#include "util/NativeUtil.hpp"

std::vector<GenericConsole*> GenericConsoleRegistry::consoles;
std::mutex GenericConsoleRegistry::consoleMutex;

boost::asio::io_context io_ctx;

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_init(JNIEnv *, jclass, const jlong jUuid, const jint width, const jint height, const jint sampleRate) {
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    return reinterpret_cast<jlong>(new GenericConsole(width, height, sampleRate, uuid));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_start(JNIEnv *env, jclass, const jlong ptr, const jstring retroCore, const jstring core, const jstring rom, const jstring save) { // NOLINT(*-misplaced-const)
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->load(env->GetStringUTFChars(retroCore, nullptr), env->GetStringUTFChars(core, nullptr),
        env->GetStringUTFChars(rom, nullptr), env->GetStringUTFChars(save, nullptr));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->dispose();
}

JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_getWidth(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    return gameboy->width;
}

JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_getHeight(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    return gameboy->height;
}

JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_getSampleRate(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    return gameboy->sampleRate;
}

GenericConsole::GenericConsole(const int width, const int height, const int sampleRate, const jUUID* uuid): width(width), height(height), sampleRate(sampleRate), uuid(uuid) {
    GenerateID(id);
    GenericConsoleRegistry::registerConsole(this);
}

void GenericConsole::load(const char *retroCore, const char *core, const char *rom, const char *save) {
    std::lock_guard lock(mutex);
    std::string strId(id, 32);
    bip::shared_memory_object::remove(strId.c_str());
    sharedMemoryHandle = new bip::managed_shared_memory(bip::create_only, strId.c_str(), 1200000);
    retroCoreHandle = sharedMemoryHandle->construct<GenericShared>("SharedData")();
    std::cout << "[RetroGamingCore] Created shared memory " << strId << std::endl;
    retroCoreProcess = new bp::process(io_ctx, retroCore, {"gn", strId, core, rom, save});
    retroCoreProcess->detach();
}

void GenericConsole::dispose() {
    std::cout << "[RetroGamingCore] Disposing bridge instance" << std::endl;
    GenericConsoleRegistry::unregisterConsole(this);
    std::lock_guard lock(mutex);
    const auto handleBackup = retroCoreHandle;
    retroCoreHandle = nullptr;
    handleBackup->shutdownRequested = true;
    while (!handleBackup->shutdownCompleted) {
        std::this_thread::yield();
    }
    std::cout << "[RetroGamingCore] Emulator shutdown completed" << std::endl;
    if (retroCoreProcess != nullptr) {
        retroCoreProcess->terminate();
        retroCoreProcess->wait();
    }
    if (sharedMemoryHandle != nullptr) {
        sharedMemoryHandle->destroy<GenericShared>("SharedData");
        std::cout << "[RetroGamingCore] Destroyed shared memory" << std::endl;
    }
    bip::shared_memory_object::remove(id);
}

std::vector<uint8_t> GenericConsole::createFrame() {
    if (videoEncoder == nullptr) {
        videoEncoder = new VideoEncoderInt16(width, height);
    }
    return videoEncoder->encodeFrame(std::vector<int16_t>(retroCoreHandle->display, retroCoreHandle->display + width * height));
}

std::vector<uint8_t> GenericConsole::createClip() {
    if (audioEncoder == nullptr) {
        audioEncoder = new AudioEncoderOpus(48000, 2, AudioEncoderOpus::Complexity::Quality);
    }
    return audioEncoder->encodeFrame(std::vector(retroCoreHandle->audio, retroCoreHandle->audio + retroCoreHandle->audioSize));
}

void GenericConsole::input(const int port, const int16_t input) const {
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

void GenericConsoleRegistry::withConsoles(const std::function<void(GenericConsole*)>& func) {
    std::lock_guard guard(consoleMutex);
    for (const auto &console : consoles) {
        console->mutex.lock();
        func(console);
        console->mutex.unlock();
    }
}

void GenericConsoleRegistry::withConsole(const jUUID *uuid, const std::function<void(GenericConsole*)> &func) {
    std::lock_guard guard(consoleMutex);
    for (const auto &console : consoles) {
        console->mutex.lock();
        if (memcmp(uuid, console->uuid, sizeof(jUUID)) == 0) {
            func(console);
            console->mutex.unlock();
            return;
        }
        console->mutex.unlock();
    }
}
