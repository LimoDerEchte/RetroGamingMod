//
// Created by limo on 2/21/25.
//

#include "NativeDisplay.hpp"

NativeDisplay::NativeDisplay(const int width, const int height) : width(width), height(height) {
    std::lock_guard lock(mutex);
    bufSize = width * height;
    buf = new uint32_t[bufSize];
    changed = new bool;
}

void NativeDisplay::receive(const uint8_t *data, const size_t size) {
    std::lock_guard lock(mutex);
    if (decoder == nullptr) {
        decoder = new VideoDecoderInt16(width, height);
    }
    decoder->decodeFrameRGB565(std::vector(data, data + size), buf);
    changed = true;
}
