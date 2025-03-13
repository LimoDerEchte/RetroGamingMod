
#pragma once
#include <mutex>

#include "codec/VideoDecoder.hpp"

class NativeDisplay {
    VideoDecoderARGB* decoder= nullptr;

public:
    mutable bool changed = false;
    uint32_t* buf{};
    size_t bufSize{};
    std::mutex mutex;
    const int width;
    const int height;

    NativeDisplay(int width, int height);

    void receive(const uint8_t* data, size_t size);
};
