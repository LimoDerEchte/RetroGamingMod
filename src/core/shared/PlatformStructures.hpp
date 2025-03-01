
#pragma once
#include <MainStructures.hpp>

struct GameBoyShared {
    const char *rom;
    NativeDisplay display = NativeDisplay(160, 144);
};
