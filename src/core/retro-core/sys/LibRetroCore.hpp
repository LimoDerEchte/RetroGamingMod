//
// Created by limo on 3/1/25.
//

#pragma once
#include <string>
#include <functional>
#include <map>
#include <mutex>

#ifdef WIN32
#include <minwindef.h>
#endif

#include "../lib/libretro-common/include/libretro.h"

class LibRetroCore {
public:
    explicit LibRetroCore(std::string corePath, std::string systemPath);
    ~LibRetroCore();
    bool loadCore();
    [[nodiscard]] bool loadROM(const std::string &romPath) const;
    void runCore() const;
    void setVideoFrameCallback(const std::function<void(const int*, unsigned, unsigned, size_t)> &callback);
    void setAudioCallback(std::function<void(const int16_t*, size_t)> const &callback);
    void setInputCallback(std::function<int16_t(unsigned port, unsigned id)> const &callback);
    void dispose() const;

    bool loadSaveFile(const char* save);
    bool saveSaveFile(const char* save);

    LibRetroCore(const LibRetroCore&) = delete;
    LibRetroCore& operator=(const LibRetroCore&) = delete;
private:
    struct {
        std::map<std::string, std::string> variables;
        bool updated;
    } env_vars;

    std::string systemPath;
    std::string corePath;
#ifdef WIN32
    HMODULE coreHandle;
#else
    void* coreHandle;
#endif
    std::mutex saveMutex;
    std::function<void(const int*, unsigned, unsigned, size_t)> videoFrameCallback;
    std::function<void(const int16_t*, size_t)> audioCallback;
    std::function<int16_t(unsigned port, unsigned id)> inputCallback;
    retro_pixel_format pixelFormat;

    typedef void (*retro_init_t)();
    typedef void (*retro_deinit_t)();
    typedef void (*retro_run_t)();
    typedef bool (*retro_load_game_t)(const retro_game_info *game);
    typedef void (*retro_unload_game_t)();
    typedef void (*retro_set_video_refresh_t)(void (*)(const void* data, unsigned width, unsigned height, size_t pitch));
    typedef void (*retro_set_environment_t)(bool (*)(unsigned cmd, void *data));
    typedef void (*retro_set_input_poll_t)(void (*)());
    typedef void (*retro_set_input_state_t)(int16_t (*)(unsigned port, unsigned device, unsigned index, unsigned id));
    typedef void (*retro_set_audio_sample_t)(void (*)(int16_t left, int16_t right));
    typedef void (*retro_set_audio_sample_batch_t)(size_t (*)(const int16_t *data, size_t frames));
    typedef void (*retro_get_system_info_t)(retro_system_info *info);
    typedef void (*retro_get_system_av_info_t)(retro_system_av_info *info);
    typedef void *(*retro_get_memory_data_t)(unsigned id);
    typedef size_t (*retro_get_memory_size_t)(unsigned id);

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
    retro_get_memory_data_t retro_get_memory_data;
    retro_get_memory_size_t retro_get_memory_size;

    void logEnvironmentVariables(const retro_variable* vars);
    static void videoRefreshCallback(const void* data, unsigned width, unsigned height, size_t pitch);
    static bool environmentCallback(unsigned cmd, void* data);
    static void inputPollCallback();
    static int16_t inputStateCallback(unsigned port, unsigned device, unsigned index, unsigned id);
    static void audioSampleCallback(int16_t left, int16_t right);
    static size_t audioSampleBatchCallback(const int16_t *data, size_t frames);
};