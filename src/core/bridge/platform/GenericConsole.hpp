//
// Created by limo on 2/28/25.
//

#pragma once
#include <boost/process.hpp>
#include <boost/interprocess/managed_shared_memory.hpp>
#include "SharedStructs.hpp"
#include "codec/VideoEncoder.hpp"

#include "util/NativeUtil.hpp"

class AudioEncoderOpus;
namespace bip = boost::interprocess;
namespace bp  = boost::process;

class GenericConsole {
    VideoEncoder* videoEncoder = nullptr;
    AudioEncoderOpus* audioEncoder = nullptr;

    bip::managed_shared_memory* sharedMemoryHandle = nullptr;
    bp::process* retroCoreProcess = nullptr;

public:
    std::mutex mutex{};
    char id[32] = {};
    const int width, height, sampleRate, codec;
    GenericShared* retroCoreHandle = nullptr;
    const jUUID* uuid;

    explicit GenericConsole(int width, int height, int sampleRate, int codec, const jUUID* uuid);

    void load(const char *retroCore, const char *core, const char *rom, const char *save);
    void dispose();

    std::vector<uint8_t> createFrame();
    std::vector<uint8_t> createClip();

    void input(int port, int16_t input) const;
};

class GenericConsoleRegistry {
    static std::vector<GenericConsole*> consoles;
    static std::mutex consoleMutex;

public:
    static void registerConsole(GenericConsole *console);
    static void unregisterConsole(GenericConsole *console);
    static void withConsoles(const std::function<void(GenericConsole*)>& func);
    static void withConsole(const jUUID* uuid, const std::function<void(GenericConsole*)>& func);
};
