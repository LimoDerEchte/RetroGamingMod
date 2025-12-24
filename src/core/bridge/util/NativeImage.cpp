//
// Created by limo on 12/23/25.
//

#include "NativeImage.hpp"

#include <cstring>
#include <memory>
#include <mutex>

NativeImage::NativeImage(const int width, const int height, uint32_t* data, const int codec) : data_(data), width_(width), height_(height), codec_(codec) {
    changed_ = true;
}

bool NativeImage::changed() const {
    return changed_;
}

uint32_t* NativeImage::nativePointer() const {
    return data_;
}

void NativeImage::receive(const std::vector<uint8_t>& data) {
    // TODO: Auto Decoder Selection
    std::lock_guard lock(mutex_);
    if (decoder_ == nullptr) {
        decoder_ = std::make_unique<VideoDecoderH264>(width_, height_);
    }
    const auto decoded = decoder_->decodeFrame(data);\
    memcpy(data_, decoded.data(), sizeof(uint32_t) * width_ * height_);
    changed_ = true;
}