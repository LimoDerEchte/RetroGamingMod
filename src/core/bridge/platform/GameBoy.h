#pragma once
#include <mutex>
#include <mgba/core/core.h>
#include <mgba/gb/core.h>

#include "util/NativeDisplay.h"

class GameBoy {
    mCore *core = GBCoreCreate();
    mColor videoBuffer[25800]{};
    bool isRunning = false;
    NativeDisplay *display = nullptr;
    std::mutex mutex;

public:
    GameBoy();
    bool load(const char *path);
    void loadSave(const char *path);
    void start();
    void stop();
    NativeDisplay *getDisplay();
    void mainLoop();
};
