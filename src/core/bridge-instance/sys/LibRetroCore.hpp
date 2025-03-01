//
// Created by limo on 3/1/25.
//

#pragma once
#include <string>
#include <functional>

#include "libretro.h"

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
    typedef void (*retro_set_environment_t)(bool (*)(unsigned cmd, void *data));
    typedef void (*retro_set_input_poll_t)(void (*)());
    typedef void (*retro_set_input_state_t)(int16_t (*)(unsigned port, unsigned device, unsigned index, unsigned id));
    typedef void (*retro_set_audio_sample_t)(void (*)(int16_t left, int16_t right));
    typedef void (*retro_set_audio_sample_batch_t)(size_t (*)(const int16_t *data, size_t frames));
    typedef void (*retro_get_system_info_t)(struct retro_system_info *info);
    typedef void (*retro_get_system_av_info_t)(struct retro_system_av_info *info);

    retro_init_t retro_init;
    retro_deinit_t retro_deinit;
    retro_run_t retro_run;
    retro_load_game_t retro_load_game;
    retro_unload_game_t retro_unload_game;
    retro_set_video_refresh_t retro_set_video_refresh;
    retro_set_environment_t retro_set_environment;
    retro_set_input_poll_t retro_set_input_poll;
    retro_set_input_state_t retro_set_input_state;
    retro_set_audio_sample_t retro_set_audio_sample;
    retro_set_audio_sample_batch_t retro_set_audio_sample_batch;
    retro_get_system_info_t retro_get_system_info;
    retro_get_system_av_info_t retro_get_system_av_info;

    static void videoRefreshCallback(const void* data, unsigned width, unsigned height, size_t pitch);
    static bool environmentCallback(unsigned cmd, void* data);
    static void inputPollCallback();
    static int16_t inputStateCallback(unsigned port, unsigned device, unsigned index, unsigned id);
    static void audioSampleCallback(int16_t left, int16_t right);
    static size_t audioSampleBatchCallback(const int16_t *data, size_t frames);
};