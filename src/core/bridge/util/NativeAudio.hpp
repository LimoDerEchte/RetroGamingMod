//
// Created by limo on 3/4/25.
//

#pragma once
#include <mutex>

struct NativeAudio {
    bool* changed{};
    int16_t *buf{};
    size_t bufSize{};
    std::mutex mutex;

    NativeAudio(bool* existingChanged, int16_t* existingBuf) {
        std::lock_guard lock(mutex);
        bufSize = 8192;
        buf = existingBuf;
        changed = existingChanged;
    }
};
