//
// Created by limo on 2/28/25.
//

#pragma once
#include <PlatformStructures.hpp>
#include <boost/process.hpp>
#include <boost/interprocess/managed_shared_memory.hpp>

#include "util/NativeDisplay.hpp"

class GameBoy {
    char id[32]{};
    NativeDisplay *display = nullptr;
    GameBoyShared *shared = nullptr;
    boost::interprocess::managed_shared_memory *segment = nullptr;
    boost::process::child *child = nullptr;

public:
    GameBoy();

    void allocate(const char *rom);
    void start();
    void dispose() const;
    NativeDisplay *getDisplay();
};
