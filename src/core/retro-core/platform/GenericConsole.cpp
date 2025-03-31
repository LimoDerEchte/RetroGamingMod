//
// Created by limo on 3/3/25.
//

#include "GenericConsole.hpp"

#include <deque>
#include <iostream>
#include <thread>
#include <bits/std_thread.h>

#include "SharedStructs.hpp"
#include "sys/LibRetroCore.hpp"

#ifndef DEBUG
//#define DEBUG
#endif

static LibRetroCore* g_instance = nullptr;
static std::deque<int16_t> g_audioBuffer;

static constexpr size_t FRAME_SIZE = 1920; // Fixed frame size (40ms at 48kHz)
static constexpr int SAVE_DELAY_SECONDS = 30;

char* getDirectory(const char* path) {
    char* dirPath = strdup(path);
    if (!dirPath) return nullptr;
    if (char* lastSlash = strrchr(dirPath, '/')) {
        *lastSlash = '\0';
    } else {
        dirPath[0] = '.';
        dirPath[1] = '\0';
    }
    return dirPath;
}

int GenericConsole::load(bip::managed_shared_memory* mem, const char *core, const char *rom, const char *save) {
    auto [gb, len] = mem->find<GenericShared>("SharedData");
    if (gb == nullptr || len == 0) {
        std::cerr << "[RetroGamingCore] The shared memory content couldn't be located" << std::endl;
        return EXIT_FAILURE;
    }
    g_instance = new LibRetroCore(core, getDirectory(core));
    g_instance->setVideoFrameCallback([gb](const int* data, const unsigned width, const unsigned height, const size_t pitch) {
        const auto pixelData = reinterpret_cast<const uint8_t*>(data);
        for (unsigned y = 0; y < height; ++y) {
            for (unsigned x = 0; x < width; ++x) {
                const uint8_t* pixel = pixelData + y * pitch + x * 2;
                const uint16_t rgb565 = pixel[0] | pixel[1] << 8;
                gb->display[y * width + x] = rgb565;
            }
        }
        gb->displayChanged = true;
    });
    g_instance->setAudioCallback([gb](const int16_t* data, const size_t pitch) {
        const size_t samples = pitch * 2;
        for (size_t i = 0; i < samples; ++i) {
            g_audioBuffer.push_back(data[i]);
        }
        if (gb->audioChanged) {
            return;
        }
        while (g_audioBuffer.size() >= FRAME_SIZE) {
            for (size_t i = 0; i < FRAME_SIZE; ++i) {
                gb->audio[i] = g_audioBuffer.front();
                g_audioBuffer.pop_front();
            }
            gb->audioSize = FRAME_SIZE;
            gb->audioChanged = true;
        }
    });
    g_instance->setInputCallback([gb](const unsigned port, const unsigned id) {
        return gb->controls[port] & 1 << id ? 0x7FFF : 0;
    });
#ifdef DEBUG
    std::cout << "[RetroGamingCore] Loading core" << std::endl;
#endif
    if (!g_instance->loadCore()) {
        std::cerr << "[RetroGamingCore] Failed to load core" << std::endl;
        return EXIT_FAILURE;
    }
#ifdef DEBUG
    std::cout << "[RetroGamingCore] Loading rom" << std::endl;
#endif
    if (!g_instance->loadROM(rom)) {
        std::cerr << "[RetroGamingCore] Failed to load ROM" << std::endl;
        return EXIT_FAILURE;
    }
#ifdef DEBUG
    std::cout << "[RetroGamingCore] Loading save" << std::endl;
#endif
    g_instance->loadSaveFile(save);
    std::cout << "[RetroGamingCore] Starting generic core" << std::endl;
    runLoops(gb, save);
#ifdef DEBUG
    std::cout << "[RetroGamingCore] Running core" << std::endl;
#endif
    g_instance->runCore();
    return EXIT_SUCCESS;
}

// ReSharper disable CppDFANullDereference
void GenericConsole::runLoops(GenericShared* shared, const char *save) {
    std::thread([save, shared] {
        const auto delay = std::chrono::seconds(SAVE_DELAY_SECONDS);
        auto nextAutoSave = std::chrono::high_resolution_clock::now() + delay;
        const auto checkDelay = std::chrono::milliseconds(10);
        auto nextCheck = std::chrono::high_resolution_clock::now() + checkDelay;
        while (true) {
            auto now = std::chrono::high_resolution_clock::now();
            bool saved = false;
            if (nextAutoSave < now) {
                g_instance->saveSaveFile(save);
                saved = true;
                nextAutoSave += delay;
            }
            if (nextCheck < now) {
                if (shared->shutdownRequested) {
                    if (!saved) {
                        g_instance->saveSaveFile(save);
                    }
                    break;
                }
                nextCheck += checkDelay;
            }
            std::this_thread::sleep_until(nextCheck);
        }
        g_instance->dispose();
        shared->shutdownCompleted = true;
    }).detach();
}
