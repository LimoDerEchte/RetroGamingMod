//
// Created by limo on 2/28/25.
//

#pragma once
#include <boost/process.hpp>
#include <boost/interprocess/managed_shared_memory.hpp>

struct GameBoyShared {
    const char *rom;
    int display[144*160] = {};

    explicit GameBoyShared(const char *rom) {
        this->rom = rom;
    }
};

class GameBoy {
    char id[32]{};
    GameBoyShared *shared = nullptr;
    boost::interprocess::managed_shared_memory *segment = nullptr;
    boost::process::child *child = nullptr;

public:
    GameBoy();

    void allocate(const char *rom);
    void start();
    void dispose() const;
};
