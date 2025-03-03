
#pragma once
#include <cstdint>

struct GameBoyShared {
    bool displayChanged = false;
    int display[240*160] = {};
    int16_t controls = 0;
};
