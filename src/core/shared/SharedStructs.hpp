
#pragma once
#include <cstdint>
#include <cstddef>

struct GameBoyShared {
    // Client to Host
    bool displayChanged = false;
    int display[240*160] = {};
    bool audioChanged = false;
    int16_t audio[8192] = {};
    size_t audioSize = 0;
    // Host to Client
    int16_t controls = 0;
};
