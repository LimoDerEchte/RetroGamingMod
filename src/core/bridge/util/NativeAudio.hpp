//
// Created by limo on 3/4/25.
//

#pragma once
#include <mutex>

struct NativeAudio {
    bool* changed{};
    int16_t *buf{};
    size_t* dataSize{};
    std::mutex mutex;

    NativeAudio(bool* existingChanged, int16_t* existingBuf, size_t* existingDataSize) {
        std::lock_guard lock(mutex);
        dataSize = existingDataSize;
        buf = existingBuf;
        changed = existingChanged;
    }
};
