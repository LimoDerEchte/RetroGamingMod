//
// Created by limo on 3/2/25.
//

#include "export.h"

#include <thread>

#include "sys/LibRetroCore.hpp"

static LibRetroCore* g_instance = nullptr;

bool coreLoad(const char *core) {
    g_instance = new LibRetroCore(core);
    return g_instance->loadCore();
}

bool romLoad(const char *rom) {
    return g_instance->loadROM(rom);
}

void updateInput(const short input) {
    g_instance->inputData = input;
}

void setVideoCallback(const std::function<void(const int*, unsigned, unsigned, size_t)> &videoCallback) {
    g_instance->setVideoFrameCallback(videoCallback);
}

void start() {
    std::thread t([] {
        g_instance->runCore();
    });
    t.detach();
}
