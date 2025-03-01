#include "LibRetroCore.hpp"
#include <dlfcn.h>
#include <iostream>
#include <thread>
#include <chrono>
#include <cstdarg>
#include "libretro.h"

static LibRetroCore* g_instance = nullptr;
static struct retro_system_info g_system_info = {0};
static struct retro_system_av_info g_av_info = {0};

// Static function for logging
static void log_printf(enum retro_log_level level, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    fprintf(stderr, "\n");
    va_end(args);
}

LibRetroCore::LibRetroCore(const std::string &corePath)
    : corePath(corePath), coreHandle(nullptr),
      retro_init(nullptr), retro_deinit(nullptr),
      retro_run(nullptr), retro_load_game(nullptr),
      retro_unload_game(nullptr), retro_set_video_refresh(nullptr),
      retro_set_environment(nullptr), retro_set_input_poll(nullptr),
      retro_set_input_state(nullptr), retro_set_audio_sample(nullptr),
      retro_set_audio_sample_batch(nullptr), retro_get_system_info(nullptr),
      retro_get_system_av_info(nullptr)
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

    // Load all required libretro functions
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

    // Check that all required functions are available
    if (!retro_init || !retro_deinit || !retro_run || !retro_load_game || !retro_unload_game ||
        !retro_set_video_refresh || !retro_set_environment || !retro_set_input_poll ||
        !retro_set_input_state || (!retro_set_audio_sample && !retro_set_audio_sample_batch) ||
        !retro_get_system_info || !retro_get_system_av_info) {
        std::cerr << "Failed to load one or more required functions." << std::endl;
        return false;
    }

    // Set environment callback first
    retro_set_environment(environmentCallback);

    // Get system info
    retro_get_system_info(&g_system_info);
    std::cout << "Loaded core: " << g_system_info.library_name << " v" << g_system_info.library_version << std::endl;

    // Set other callbacks
    retro_set_video_refresh(videoRefreshCallback);
    retro_set_input_poll(inputPollCallback);
    retro_set_input_state(inputStateCallback);

    // Set at least one audio callback
    if (retro_set_audio_sample)
        retro_set_audio_sample(audioSampleCallback);
    if (retro_set_audio_sample_batch)
        retro_set_audio_sample_batch(audioSampleBatchCallback);

    // Initialize the core
    retro_init();
    return true;
}

bool LibRetroCore::loadROM(const std::string &romPath) const {
    // Create game info structure with proper ROM path
    retro_game_info gameInfo = {
        romPath.c_str(),  // path
        nullptr,          // data (we're not loading from memory)
        0,                // size (not used when loading from file)
        nullptr           // meta (not used)
    };

    // Load the game
    if (!retro_load_game(&gameInfo)) {
        std::cerr << "Failed to load ROM: " << romPath << std::endl;
        return false;
    }

    // Get AV info for proper rendering
    retro_get_system_av_info(&g_av_info);
    std::cout << "Game loaded. Resolution: " << g_av_info.geometry.base_width << "x"
              << g_av_info.geometry.base_height << " @ " << g_av_info.timing.fps << " fps" << std::endl;

    return true;
}

void LibRetroCore::runCore() const {
    try {
        // Calculate frame time based on core's reported FPS (or default to 60fps)
        float fps = g_av_info.timing.fps > 0 ? g_av_info.timing.fps : 60.0f;
        const std::chrono::microseconds frametime(static_cast<int>(1000000.0f / fps));

        std::cout << "Starting main loop at " << fps << " fps (frame time: "
                  << frametime.count() << "μs)" << std::endl;

        while (true) {
            auto start = std::chrono::high_resolution_clock::now();

            // Poll inputs
            inputPollCallback();

            // Run one frame
            retro_run();

            // Frame timing
            auto end = std::chrono::high_resolution_clock::now();
            auto elapsed = std::chrono::duration_cast<std::chrono::microseconds>(end - start);
            if (elapsed < frametime) {
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

void LibRetroCore::videoRefreshCallback(const void* data, const unsigned width, const unsigned height, const size_t pitch) {
    if (!g_instance || !data)
        return;

    if (g_instance->videoFrameCallback) {
        g_instance->videoFrameCallback(static_cast<const int*>(data), width, height, pitch);
    }
}

bool LibRetroCore::environmentCallback(unsigned cmd, void* data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: {
            struct retro_log_callback *cb = (struct retro_log_callback*)data;
            // We use a standalone function instead of a lambda
            cb->log = log_printf;
            return true;
        }

        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            *static_cast<bool*>(data) = true;
            return true;

        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            const enum retro_pixel_format *fmt = (enum retro_pixel_format *)data;
            if (*fmt > RETRO_PIXEL_FORMAT_RGB565) {
                return false; // Unsupported pixel format
            }
            return true;
        }

        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY: {
            *(const char**)data = "."; // Use current directory
            return true;
        }

        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_GET_VARIABLE:
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            // Just accept these but don't do anything with them
            return true;
    }

    return false; // Unhandled command
}

void LibRetroCore::inputPollCallback() {
    // No-op
}

int16_t LibRetroCore::inputStateCallback(unsigned port, unsigned device, unsigned index, unsigned id) {
    return 0; // No buttons pressed
}

void LibRetroCore::audioSampleCallback(int16_t left, int16_t right) {
    // Discard audio samples
}

size_t LibRetroCore::audioSampleBatchCallback(const int16_t *data, size_t frames) {
    return frames; // Pretend all frames were processed
}