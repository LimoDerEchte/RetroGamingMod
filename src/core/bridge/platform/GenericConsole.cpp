
#include "GenericConsole.hpp"

#include <algorithm>
#include <cstring>
#include <iostream>
#include <jni.h>
#include <mutex>
#include <thread>
#include <chrono>
#include <reproc++/run.hpp>

#include <headers/com_limo_emumod_bridge_NativeGenericConsole.h>
#include <codec/AudioEncoder.hpp>
#include <util/NativeUtil.hpp>

std::vector<GenericConsole*> GenericConsoleRegistry::consoles;
std::shared_mutex GenericConsoleRegistry::consoleMutex;

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_init(JNIEnv *, jclass, const jlong jUuid, const jlong jConsoleId, const jint width, const jint height, const jint sampleRate, const jint codec) {
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    const auto consoleId = reinterpret_cast<jUUID*>(jConsoleId);
    // ReSharper disable once CppDFAMemoryLeak
    return reinterpret_cast<jlong>(new GenericConsole(width, height, sampleRate, codec, uuid, consoleId));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_start(JNIEnv *env, jclass, const jlong ptr, const jstring retroCore, const jstring core, const jstring rom, const jstring save) { // NOLINT(*-misplaced-const)
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->load(env->GetStringUTFChars(retroCore, nullptr), env->GetStringUTFChars(core, nullptr),
        env->GetStringUTFChars(rom, nullptr), env->GetStringUTFChars(save, nullptr));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GenericConsole*>(ptr);
    gameboy->dispose();
    delete gameboy;
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

GenericConsole::GenericConsole(const int width, const int height, const int sampleRate, const int codec, const jUUID* uuid, const jUUID* consoleId)
                                : width(width), height(height), sampleRate(sampleRate), codec(codec), uuid(uuid), consoleId(consoleId) {
    GenerateID(id);
    GenericConsoleRegistry::registerConsole(this);
}

void GenericConsole::load(const char *retroCore, const char *core, const char *rom, const char *save) {
    std::lock_guard lock(mutex);
    const std::string strId(id, 32);

    boost::interprocess::shared_memory_object::remove(strId.c_str());
    sharedMemoryHandle = new boost::interprocess::managed_shared_memory(boost::interprocess::create_only, strId.c_str(), 1200000);
    retroCoreHandle = sharedMemoryHandle->construct<GenericShared>("SharedData")();

    std::cout << "[RetroGamingCore] Created shared memory " << strId << std::endl;

    reproc::options options{};
    //options.nonblocking = true;

    const char* args[] = { retroCore, "gn", strId.c_str(), core, rom, save, nullptr };
    if (const std::error_code err = retroCoreProcess.start(args, options)) {
        std::cerr << "[RetroGamingCore] Failed to start process: " << err.message() << std::endl;
    }
}

void GenericConsole::dispose() {
    std::cout << "[RetroGamingCore] Disposing bridge instance" << std::endl;
    GenericConsoleRegistry::unregisterConsole(this);

    std::lock_guard lock(mutex);
    const auto handleBackup = retroCoreHandle;
    retroCoreHandle = nullptr;

    if (auto [status, ec] = retroCoreProcess.wait(reproc::milliseconds(0)); ec == std::errc::timed_out) {

        if (handleBackup != nullptr) {
            handleBackup->shutdownRequested = true;
            const auto start = std::chrono::steady_clock::now();
            while (!handleBackup->shutdownCompleted) {
                if (std::chrono::steady_clock::now() - start > std::chrono::seconds(10)) {
                    std::cerr << "[RetroGamingCore] Emulator shutdown timed out" << std::endl;
                    break;
                }
                std::this_thread::yield();
            }
        }

        constexpr reproc::stop_actions stop = {
            { reproc::stop::noop, reproc::milliseconds(0) },
            { reproc::stop::terminate, reproc::milliseconds(5000) },
            { reproc::stop::kill, reproc::milliseconds(2000) }
        };
        retroCoreProcess.stop(stop);

        std::cout << "[RetroGamingCore] Emulator shutdown completed" << std::endl;
    }

    if (sharedMemoryHandle != nullptr) {
        sharedMemoryHandle->destroy<GenericShared>("SharedData");
        std::cout << "[RetroGamingCore] Destroyed shared memory" << std::endl;
    }
    boost::interprocess::shared_memory_object::remove(id);
}

std::vector<uint8_t> GenericConsole::createFrame() {
    if (videoEncoder == nullptr) {
        switch (codec) {
            case 0:
                videoEncoder = std::make_unique<VideoEncoderAV1>(width, height);
                break;
            default:
                return {};
        }
    }
    return videoEncoder->encodeFrameRGB565(std::vector<int16_t>(retroCoreHandle->display, retroCoreHandle->display + width * height));
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
    std::unique_lock guard(consoleMutex);
    consoles.emplace_back(console);
}

void GenericConsoleRegistry::unregisterConsole(GenericConsole *console) {
    std::lock_guard guard(consoleMutex);
    consoles.erase(std::ranges::remove(consoles, console).begin(), consoles.end());
}

void GenericConsoleRegistry::withConsoles(const bool writing, const std::function<void(GenericConsole*)>& func) {
    if (writing)
        std::unique_lock guard(consoleMutex);
    else
        std::shared_lock guard(consoleMutex);

    for (const auto &console : consoles) {
        console->mutex.lock();
        func(console);
        console->mutex.unlock();
    }
}

void GenericConsoleRegistry::withConsole(const bool writing, const jUUID *uuid, const std::function<void(GenericConsole*)> &func) {
    if (writing)
        std::unique_lock guard(consoleMutex);
    else
        std::shared_lock guard(consoleMutex);

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
