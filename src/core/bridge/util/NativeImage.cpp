//
// Created by limo on 12/23/25.
//

#include "NativeImage.hpp"

#include <memory>
#include <mutex>

NativeImage::NativeImage(const int width, const int height, uint32_t* data) : data_(data), width_(width), height_(height) {
    changed_ = true;
}

bool NativeImage::changed() const {
    return changed_;
}

uint32_t* NativeImage::nativePointer() const {
    return data_;
}

void NativeImage::receive(const std::vector<uint8_t>& data) {
    std::lock_guard lock(mutex_);
    if (decoder_ == nullptr) {
        decoder_ = std::make_unique<VideoDecoderInt16>(width_, height_);
    }
    decoder_->decodeFrameRGB565(data, data_);
    changed_ = true;
}
