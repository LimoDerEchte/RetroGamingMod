#pragma once
#include <mgba/internal/gb/gb.h>

class GameBoy {
    GB *gb = nullptr;

public:
    void load(const char *path);
    void start();
    void stop();
};
