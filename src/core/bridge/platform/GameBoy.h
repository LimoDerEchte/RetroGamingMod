#pragma once
#include <mgba/core/core.h>
#include <mgba/gb/core.h>

class GameBoy {
    mCore *core = GBCoreCreate();

public:
    void load(const char *path);
    void start();
    void stop() const;
};
