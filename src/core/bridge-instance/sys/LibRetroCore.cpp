#include "LibRetroCore.hpp"
#include <dlfcn.h>
#include <iostream>
#include "libretro.h"

static LibRetroCore* g_instance = nullptr;

LibRetroCore::LibRetroCore(const std::string &corePath)
    : corePath(corePath), coreHandle(nullptr),
      retro_init(nullptr), retro_deinit(nullptr),
      retro_run(nullptr), retro_load_game(nullptr),
      retro_unload_game(nullptr), retro_set_video_refresh(nullptr)
{
    g_instance = this;
}

LibRetroCore::~LibRetroCore() {
    if (retro_unload_game)
        retro_unload_game();
    if (retro_deinit)
        retro_deinit();
    if (coreHandle)
        dlclose(coreHandle);
}

bool LibRetroCore::loadCore() {
    coreHandle = dlopen(corePath.c_str(), RTLD_LAZY);
    if (!coreHandle) {
        std::cerr << "Failed to load core: " << dlerror() << std::endl;
        return false;
    }

    // ReSharper disable CppCStyleCast
    retro_init = (retro_init_t) dlsym(coreHandle, "retro_init");
    retro_deinit = (retro_deinit_t) dlsym(coreHandle, "retro_deinit");
    retro_run = (retro_run_t) dlsym(coreHandle, "retro_run");
    retro_load_game = (retro_load_game_t) dlsym(coreHandle, "retro_load_game");
    retro_unload_game = (retro_unload_game_t) dlsym(coreHandle, "retro_unload_game");
    retro_set_video_refresh = (retro_set_video_refresh_t) dlsym(coreHandle, "retro_set_video_refresh");

    if (!retro_init || !retro_deinit || !retro_run || !retro_load_game || !retro_unload_game || !retro_set_video_refresh) {
        std::cerr << "Failed to load one or more required functions." << std::endl;
        return false;
    }

    retro_set_video_refresh(videoRefreshCallback);
    retro_init();
    return true;
}

bool LibRetroCore::loadROM(const std::string &romPath) const {
    const retro_game_info gameInfo = { romPath.c_str(), nullptr };
    if (!retro_load_game(&gameInfo)) {
        std::cerr << "Failed to load ROM: " << romPath << std::endl;
        return false;
    }
    return true;
}

[[noreturn]] void LibRetroCore::runCore() const {
    for (;;) {
        retro_run();
    }
}

void LibRetroCore::setVideoFrameCallback(const std::function<void(const int*, unsigned, unsigned, size_t)> &callback) {
    videoFrameCallback = callback;
}

void LibRetroCore::videoRefreshCallback(const void* data, const unsigned width, const unsigned height, const size_t pitch) {
    if (!g_instance)
        return;

    if (g_instance->videoFrameCallback) {
        g_instance->videoFrameCallback(static_cast<const int*>(data), width, height, pitch);
    }
}
