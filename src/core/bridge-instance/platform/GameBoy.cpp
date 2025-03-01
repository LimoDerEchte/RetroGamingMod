//
// Created by limo on 3/1/25.
//

#include "GameBoy.hpp"

#include <iostream>
#include <PlatformStructures.hpp>

#include "sys/LibRetroCore.hpp"
#include "sys/paths.h"

namespace bip = boost::interprocess;

int GB::init(const std::string &id, bip::managed_shared_memory *segment) {
    auto [shared, size] = segment->find<GameBoyShared>("SharedData");
    if (shared == nullptr || size == 0) {
        std::cerr << "[RetroGamingCore | " << id << "] Error: SharedData not found!" << std::endl;
        return EXIT_FAILURE;
    }
    LibRetroCore core(GB_CORE_PATH);
    if (!core.loadCore()) {
        std::cerr << "[RetroGamingCore | " << id << "] Error: Failed to load core!" << std::endl;
        return EXIT_FAILURE;
    }
    if (!core.loadROM(shared->rom)) {
        std::cerr << "[RetroGamingCore | " << id << "] Error: Failed to load ROM!" << std::endl;
        return EXIT_FAILURE;
    }
    core.setVideoFrameCallback([shared](const int* data, const unsigned width, const unsigned height, const size_t pitch) {
        const auto pixelData = reinterpret_cast<const uint8_t*>(data);
        for (unsigned y = 0; y < height; ++y) {
            for (unsigned x = 0; x < width; ++x) {
                const uint8_t* pixel = pixelData + y * pitch + x * 4;
                const uint8_t r = pixel[1];
                const uint8_t g = pixel[2];
                const uint8_t b = pixel[3];
                constexpr uint8_t a = 0xFF;
                shared->display.buf[y * width + x] = a << 24 | r << 16 | g << 8 | b;
                shared->display.changed = true;
            }
        }
    });
    std::cout << "[RetroGamingCore | " << id << "] Successfully loaded - running core..." << std::endl;
    core.runCore();
    return EXIT_SUCCESS;
}

