//
// Created by limo on 2/28/25.
//

#pragma once
#include <boost/process.hpp>
#include <boost/interprocess/managed_shared_memory.hpp>
#include "SharedStructs.hpp"
#include "util/NativeDisplay.hpp"

#include "util/NativeAudio.hpp"
#include "util/NativeUtil.hpp"

namespace bip = boost::interprocess;
namespace bp  = boost::process;

class GenericConsole {
    std::mutex mutex{};
    NativeDisplay* nativeDisplay = nullptr;
    NativeAudio* nativeAudio = nullptr;

    bip::managed_shared_memory* sharedMemoryHandle = nullptr;
    bp::child* retroCoreProcess = nullptr;

public:
    char id[32] = {};
    const int width, height;
    GenericShared* retroCoreHandle = nullptr;
    const jUUID* uuid;

    explicit GenericConsole(int width, int height, const jUUID* uuid);

    void load(const char *retroCore, const char *core, const char *rom);
    void dispose();
    [[nodiscard]] NativeDisplay *getDisplay() const;
    [[nodiscard]] NativeAudio *getAudio() const;

    void input(int port, int16_t input);
};

class GenericConsoleRegistry {
    static std::vector<GenericConsole*> consoles;
    static std::mutex consoleMutex;

public:
    static void registerConsole(GenericConsole *console);
    static void unregisterConsole(GenericConsole *console);
    static void withConsoles(const std::function<void(GenericConsole*)>& func);
};
