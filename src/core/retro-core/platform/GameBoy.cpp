//
// Created by limo on 3/3/25.
//

#include "GameBoy.hpp"

#include <iostream>

#include "SharedStructs.hpp"
#include "sys/LibRetroCore.hpp"

static LibRetroCore* g_instance = nullptr;

int GB::load(bip::managed_shared_memory* mem, const char *core, const char *rom) {
    auto [gb, len] = mem->find<GameBoyShared>("SharedData");
    if (gb == nullptr || len == 0) {
        std::cerr << "[RetroGamingCore] The shared memory data couldn't be located" << std::endl;
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
                const uint8_t r = (rgb565 >> 11 & 0x1F) << 3;
                const uint8_t g = (rgb565 >> 5 & 0x3F) << 2;
                const uint8_t b = (rgb565 & 0x1F) << 3;
                constexpr uint8_t a = 0xFF;
                gb->display[y * width + x] = a << 24 | r << 16 | g << 8 | b;
                gb->displayChanged = true;
            }
        }
        g_instance->inputData = gb->controls;
    });
    g_instance->setAudioCallback([gb](const int16_t* data, size_t pitch) {
        if (pitch > 4096) {
            pitch = 4096;
        }
        memcpy(gb->audio, data, 4 * pitch);
        gb->audioSize = 2 * pitch;
        gb->audioChanged = true;
    });
    std::cout << "[RetroGamingCore] Starting GameBoy core" << std::endl;
    g_instance->runCore();
    return EXIT_SUCCESS;
}
