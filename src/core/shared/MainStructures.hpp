//
// Created by limo on 3/1/25.
//

#pragma once
#include <mutex>

struct NativeDisplay {
    bool changed{};
    int *buf{};
    size_t bufSize{};
    std::mutex mutex;

    NativeDisplay(const int width, const int height) {
        std::lock_guard lock(mutex);
        bufSize = width * height;
        buf = new int[bufSize];
        changed = false;
    }
};
