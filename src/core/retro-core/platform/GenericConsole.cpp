//
// Created by limo on 3/3/25.
//

#include "GenericConsole.hpp"

#include <iostream>

#include "SharedStructs.hpp"
#include "sys/LibRetroCore.hpp"

static LibRetroCore* g_instance = nullptr;

int GenericConsole::load(bip::managed_shared_memory* mem, const char *core, const char *rom) {
    auto [gb, len] = mem->find<GenericShared>("SharedData");
    if (gb == nullptr || len == 0) {
        std::cerr << "[RetroGamingCore] The shared memory content couldn't be located" << std::endl;
        return EXIT_FAILURE;
    }
    g_instance = new LibRetroCore(core);
    if (!g_instance->loadCore()) {
        std::cerr << "[RetroGamingCore] Failed to load core" << std::endl;
        return EXIT_FAILURE;
    }
    if (!g_instance->loadROM(rom)) {
        std::cerr << "[RetroGamingCore] Failed to load ROM" << std::endl;
        return EXIT_FAILURE;
    }
    g_instance->setVideoFrameCallback([gb](const int* data, const unsigned width, const unsigned height, const size_t pitch) {
        const auto pixelData = reinterpret_cast<const uint8_t*>(data);
        for (unsigned y = 0; y < height; ++y) {
            for (unsigned x = 0; x < width; ++x) {
                const uint8_t* pixel = pixelData + y * pitch + x * 2;
                const uint16_t rgb565 = pixel[0] | pixel[1] << 8;
                gb->display[y * width + x] = rgb565;
                gb->displayChanged = true;
            }
        }
    });
    g_instance->setAudioCallback([gb](const int16_t* data, size_t pitch) {
        if (pitch > 4096) {
            pitch = 4096;
        }
        if (2 * pitch + gb->audioSize > 4096) {
            memcpy(gb->audio, data, 4 * pitch);
            gb->audioSize = 2 * pitch;
        } else {
            memcpy(&gb->audio[gb->audioSize / 2], data, 2 * pitch);
            gb->audioSize += 2 * pitch;
        }
        gb->audioChanged = gb->audioSize > 4096;
    });
    g_instance->setInputCallback([gb](const unsigned port, const unsigned id) {
        return gb->controls[port] & 1 << id ? 0x7FFF : 0;
    });
    std::cout << "[RetroGamingCore] Starting generic core" << std::endl;
    g_instance->runCore();
    return EXIT_SUCCESS;
}
