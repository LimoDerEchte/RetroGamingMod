//
// Created by limo on 2/28/25.
//

#pragma once
#include <PlatformStructures.hpp>
#include <boost/process.hpp>
#include <boost/interprocess/managed_shared_memory.hpp>


struct NativeDisplay;

class GameBoy {
    GameBoyShared *shared = nullptr;
    boost::interprocess::managed_shared_memory *segment = nullptr;
    boost::process::child *child = nullptr;

public:
    char id[32]{};
    GameBoy();

    void allocate(const char *rom);
    void start();
    void dispose() const;
    NativeDisplay *getDisplay() const;
};
