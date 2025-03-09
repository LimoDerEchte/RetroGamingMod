
#pragma once
#include <mutex>

struct NativeDisplay {
    bool* changed{};
    uint16_t* buf{};
    size_t bufSize{};
    std::mutex mutex;
    const int width;
    const int height;

    NativeDisplay(const int width, const int height, bool* existingChanged, uint16_t* existingBuf): width(width),
        height(height) {
        std::lock_guard lock(mutex);
        bufSize = width * height;
        buf = existingBuf;
        changed = existingChanged;
    }
};