//
// Created by limo on 2/28/25.
//

#pragma once
#include "SharedStructs.hpp"
#include "util/NativeDisplay.hpp"
#include <boost/interprocess/managed_shared_memory.hpp>
#include <boost/process.hpp>

#include "util/NativeAudio.hpp"

namespace bip = boost::interprocess;
namespace bp  = boost::process;

class GameBoy {
    std::mutex mutex{};
    NativeDisplay* nativeDisplay = nullptr;
    NativeAudio* nativeAudio = nullptr;

    char id[32] = {};
    GameBoyShared* retroCoreHandle = nullptr;
    bip::managed_shared_memory* sharedMemoryHandle = nullptr;
    bp::child* retroCoreProcess = nullptr;
    const bool isGba;

public:
    explicit GameBoy(bool isGBA);

    void load(const char *retroCore, const char *core, const char *rom);
    void dispose();
    [[nodiscard]] NativeDisplay *getDisplay() const;
    [[nodiscard]] NativeAudio *getAudio() const;

    void input(int16_t input);
};
