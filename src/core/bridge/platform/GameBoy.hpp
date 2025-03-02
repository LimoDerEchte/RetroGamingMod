//
// Created by limo on 2/28/25.
//

#pragma once

#include "sys/LibRetroCore.hpp"
#include "util/NativeDisplay.hpp"

class GameBoy {
    std::mutex mutex{};
    LibRetroCore* libRetroCore = nullptr;
    NativeDisplay* nativeDisplay = new NativeDisplay(160, 144);

public:
    void load(const char *rom);
    void start();
    void dispose();
    [[nodiscard]] NativeDisplay *getDisplay() const;
};
