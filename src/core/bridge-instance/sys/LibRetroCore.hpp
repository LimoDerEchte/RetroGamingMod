//
// Created by limo on 3/1/25.
//

#pragma once
#include <string>
#include <functional>

class LibRetroCore {
public:
    explicit LibRetroCore(const std::string &corePath);
    ~LibRetroCore();
    bool loadCore();
    bool loadROM(const std::string &romPath) const;
    void runCore() const;
    void setVideoFrameCallback(const std::function<void(const int*, unsigned, unsigned, size_t)> &callback);

private:
    LibRetroCore(const LibRetroCore&) = delete;
    LibRetroCore& operator=(const LibRetroCore&) = delete;

    std::string corePath;
    void* coreHandle;
    std::function<void(const int*, unsigned, unsigned, size_t)> videoFrameCallback;

    typedef void (*retro_init_t)();
    typedef void (*retro_deinit_t)();
    typedef void (*retro_run_t)();
    typedef bool (*retro_load_game_t)(const struct retro_game_info *game);
    typedef void (*retro_unload_game_t)();
    typedef void (*retro_set_video_refresh_t)(void (*)(const void* data, unsigned width, unsigned height, size_t pitch));

    retro_init_t retro_init;
    retro_deinit_t retro_deinit;
    retro_run_t retro_run;
    retro_load_game_t retro_load_game;
    retro_unload_game_t retro_unload_game;
    retro_set_video_refresh_t retro_set_video_refresh;

    static void videoRefreshCallback(const void* data, unsigned width, unsigned height, size_t pitch);
};
