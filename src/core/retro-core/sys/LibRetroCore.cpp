#include "LibRetroCore.hpp"
#include <dlfcn.h>
#include <iostream>
#include <thread>
#include <chrono>
#include <cstdarg>
#include <utility>

static LibRetroCore* g_instance = nullptr;
static retro_system_info g_system_info = {nullptr};
static retro_system_av_info g_av_info = {};

static void log_printf(retro_log_level level, const char *fmt, ...) {
    /* Ignore logs to prevent spamming for now
    va_list args;
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    fprintf(stderr, "\n");
    va_end(args);*/
}

LibRetroCore::LibRetroCore(std::string corePath)
    : corePath(std::move(corePath)), coreHandle(nullptr), retro_init(nullptr), retro_deinit(nullptr),
      retro_run(nullptr), retro_load_game(nullptr), retro_unload_game(nullptr), retro_set_video_refresh(nullptr),
      retro_set_environment(nullptr), retro_set_input_poll(nullptr), retro_set_input_state(nullptr),
      retro_set_audio_sample(nullptr), retro_set_audio_sample_batch(nullptr), retro_get_system_info(nullptr),
      retro_get_system_av_info(nullptr), retro_get_memory_data(nullptr), retro_get_memory_size(nullptr)
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
    coreHandle = dlopen(corePath.c_str(), RTLD_LOCAL | RTLD_LAZY);
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
    retro_set_environment = (retro_set_environment_t) dlsym(coreHandle, "retro_set_environment");
    retro_set_input_poll = (retro_set_input_poll_t) dlsym(coreHandle, "retro_set_input_poll");
    retro_set_input_state = (retro_set_input_state_t) dlsym(coreHandle, "retro_set_input_state");
    retro_set_audio_sample = (retro_set_audio_sample_t) dlsym(coreHandle, "retro_set_audio_sample");
    retro_set_audio_sample_batch = (retro_set_audio_sample_batch_t) dlsym(coreHandle, "retro_set_audio_sample_batch");
    retro_get_system_info = (retro_get_system_info_t) dlsym(coreHandle, "retro_get_system_info");
    retro_get_system_av_info = (retro_get_system_av_info_t) dlsym(coreHandle, "retro_get_system_av_info");
    retro_get_memory_data = (retro_get_memory_data_t) dlsym(coreHandle, "retro_get_memory_data");
    retro_get_memory_size = (retro_get_memory_size_t) dlsym(coreHandle, "retro_get_memory_size");

    if (!retro_init || !retro_deinit || !retro_run || !retro_load_game || !retro_unload_game ||
        !retro_set_video_refresh || !retro_set_environment || !retro_set_input_poll ||
        !retro_set_input_state || (!retro_set_audio_sample && !retro_set_audio_sample_batch) ||
        !retro_get_system_info || !retro_get_system_av_info) {
        std::cerr << "Failed to load one or more required functions." << std::endl;
        return false;
    }

    retro_set_environment(environmentCallback);

    retro_get_system_info(&g_system_info);
    std::cout << "Loaded core: " << g_system_info.library_name << " v" << g_system_info.library_version << std::endl;

    retro_set_video_refresh(videoRefreshCallback);
    retro_set_input_poll(inputPollCallback);
    retro_set_input_state(inputStateCallback);

    if (retro_set_audio_sample)
        retro_set_audio_sample(audioSampleCallback);
    if (retro_set_audio_sample_batch)
        retro_set_audio_sample_batch(audioSampleBatchCallback);

    retro_init();
    return true;
}

bool LibRetroCore::loadROM(const std::string &romPath) const {
    const retro_game_info gameInfo = {
        romPath.c_str(),
        nullptr,
        0,
        nullptr
    };
    if (!retro_load_game(&gameInfo)) {
        std::cerr << "Failed to load ROM: " << romPath << std::endl;
        return false;
    }
    retro_get_system_av_info(&g_av_info);
    std::cout << "Game loaded. Resolution: " << g_av_info.geometry.base_width << "x"
              << g_av_info.geometry.base_height << " @ " << g_av_info.timing.fps << " fps" << std::endl;
    return true;
}

void LibRetroCore::runCore() const {
    try {
        const double fps = g_av_info.timing.fps > 0 ? g_av_info.timing.fps : 60.0f;
        const std::chrono::microseconds frametime(static_cast<int>(1000000.0f / fps));
        std::cout << "Starting main loop at " << fps << " fps (frame time: "
                  << frametime.count() << "μs)" << std::endl;
        while (true) {
            auto start = std::chrono::high_resolution_clock::now();
            retro_run();
            auto end = std::chrono::high_resolution_clock::now();
            if (auto elapsed = std::chrono::duration_cast<std::chrono::microseconds>(end - start); elapsed < frametime) {
                std::this_thread::sleep_for(frametime - elapsed);
            }
        }
    } catch (const std::exception& e) {
        std::cerr << "Exception in runCore: " << e.what() << std::endl;
    } catch (...) {
        std::cerr << "Unknown exception in runCore" << std::endl;
    }
}

void LibRetroCore::setVideoFrameCallback(const std::function<void(const int*, unsigned, unsigned, size_t)> &callback) {
    videoFrameCallback = callback;
}

void LibRetroCore::setAudioCallback(const std::function<void(const int16_t*, size_t)> &callback) {
    audioCallback = callback;
}

void LibRetroCore::setInputCallback(const std::function<int16_t(unsigned port, unsigned id)> &callback) {
    inputCallback = callback;
}

void LibRetroCore::dispose() const {
    retro_unload_game();
    retro_deinit();
    if (coreHandle)
        dlclose(coreHandle);
}

void LibRetroCore::videoRefreshCallback(const void* data, const unsigned width, const unsigned height, const size_t pitch) {
    if (g_instance && g_instance->videoFrameCallback) {
        g_instance->videoFrameCallback(static_cast<const int*>(data), width, height, pitch);
    }
}

bool LibRetroCore::environmentCallback(const unsigned cmd, void* data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: {
            auto *cb = static_cast<struct retro_log_callback *>(data);
            cb->log = log_printf;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            *static_cast<bool*>(data) = true;
            return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            if (const retro_pixel_format *fmt = static_cast<enum retro_pixel_format *>(data); *fmt > RETRO_PIXEL_FORMAT_RGB565) {
                return false;
            }
            return true;
        }
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY: {
            *static_cast<const char **>(data) = ".";
            return true;
        }
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_GET_VARIABLE:
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            return true;
        default:
            return false;
    }
}

void LibRetroCore::inputPollCallback() {
    // Nothing to do
}

int16_t LibRetroCore::inputStateCallback(const unsigned port, unsigned device, unsigned index, const unsigned id) {
    if (!g_instance->inputCallback)
        return 0;
    return g_instance->inputCallback(port, id);
}

void LibRetroCore::audioSampleCallback(int16_t left, int16_t right) {
    // Nothing to do
}

size_t LibRetroCore::audioSampleBatchCallback(const int16_t *data, const size_t frames) {
    if(g_instance && g_instance->audioCallback)
        g_instance->audioCallback(data, frames);
    return frames;
}