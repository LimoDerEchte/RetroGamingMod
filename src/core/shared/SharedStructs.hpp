
#pragma once
#include <cstdint>

struct GameBoyShared {
    bool displayChanged = false;
    int display[240*160] = {};
    bool audioChanged = false;
    int16_t audio[8192] = {};
    int16_t controls = 0;
};
