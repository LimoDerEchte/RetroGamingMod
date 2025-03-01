
#pragma once
#include <cstring>
#include <MainStructures.hpp>

struct GameBoyShared {
    char rom[512];
    NativeDisplay display = NativeDisplay(160, 144);

    void setRom(const char* path) {
        if (path) {
            strncpy(rom, path, sizeof(rom) - 1);
            rom[sizeof(rom) - 1] = '\0';
        }
    }
};
