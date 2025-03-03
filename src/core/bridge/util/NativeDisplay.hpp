
#pragma once
#include <mutex>

struct NativeDisplay {
    bool* changed{};
    int *buf{};
    size_t bufSize{};
    std::mutex mutex;

    NativeDisplay(const int width, const int height, bool* existingChanged, int* existingBuf) {
        std::lock_guard lock(mutex);
        bufSize = width * height;
        buf = existingBuf;
        changed = existingChanged;
    }
};