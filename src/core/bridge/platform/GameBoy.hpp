//
// Created by limo on 2/28/25.
//

#pragma once

#include <functional>

#include "util/NativeDisplay.hpp"

class GameBoy {
    std::mutex mutex{};
    void* retroCoreHandle = nullptr;
    NativeDisplay* nativeDisplay;

    typedef bool (*core_load_t)(const char* core);
    typedef bool (*rom_load_t)(const char* rom);
    typedef void (*update_input_t)(short input);
    typedef void (*video_callback_t)(std::function<void(const int*, unsigned, unsigned, size_t)> videoCallback);
    typedef void (*start_t)();

    core_load_t coreLoad = nullptr;
    rom_load_t romLoad = nullptr;
    update_input_t updateInput = nullptr;
    video_callback_t videoCallback = nullptr;
    start_t startGame = nullptr;

public:
    explicit GameBoy(bool isGBA);

    void load(const char *retroCore, const char *core, const char *rom);
    void start();
    void dispose();
    [[nodiscard]] NativeDisplay *getDisplay() const;
    void input(int16_t input);
};
