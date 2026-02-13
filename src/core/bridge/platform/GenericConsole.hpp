
#pragma once

#include <functional>
#include <shared_mutex>
#include <SharedStructs.hpp>
#include <boost/interprocess/managed_shared_memory.hpp>
#include <codec/VideoEncoder.hpp>
#include <util/NativeUtil.hpp>
#include <reproc++/reproc.hpp>

class AudioEncoderOpus;

class GenericConsole {
    std::unique_ptr<VideoEncoder> videoEncoder = nullptr;
    AudioEncoderOpus* audioEncoder = nullptr;

    boost::interprocess::managed_shared_memory* sharedMemoryHandle = nullptr;
    reproc::process retroCoreProcess;

public:
    std::mutex mutex{};
    char id[33] = {};
    const int width, height, sampleRate, codec;
    GenericShared* retroCoreHandle = nullptr;
    const jUUID* uuid;
    const jUUID* consoleId;

    explicit GenericConsole(int width, int height, int sampleRate, int codec, const jUUID* uuid, const jUUID* consoleId);

    void load(const char *retroCore, const char *core, const char *rom, const char *save);
    void dispose();

    std::vector<uint8_t> createFrame();
    std::vector<uint8_t> createClip();

    void input(int port, int16_t input) const;
};

class GenericConsoleRegistry {
    static std::vector<GenericConsole*> consoles;
    static std::shared_mutex consoleMutex;

public:
    static void registerConsole(GenericConsole *console);
    static void unregisterConsole(GenericConsole *console);
    static void withConsoles(bool writing, const std::function<void(GenericConsole *)> &func);
    static void withConsole(bool writing, const jUUID *uuid, const std::function<void(GenericConsole *)> &func);
};
