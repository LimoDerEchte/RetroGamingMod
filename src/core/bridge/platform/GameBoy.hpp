//
// Created by limo on 2/28/25.
//

#pragma once

class GameBoy {
    char id[32]{};

public:
    GameBoy();
    void allocate(const char *rom);
    void start();
    void dispose();
};

struct GameBoyShared {
    const char *rom;
    int display[144*160] = {};

    explicit GameBoyShared(const char *rom) {
        this->rom = rom;
    }
};
