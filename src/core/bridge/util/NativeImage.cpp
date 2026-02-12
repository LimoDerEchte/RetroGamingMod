//
// Created by limo on 12/23/25.
//

#include "NativeImage.hpp"

#include <cstring>
#include <iostream>
#include <memory>
#include <mutex>

NativeImage::NativeImage(const int width, const int height, uint32_t* data, const int codec, const int sampleRate)
                            : data_(data), width(width), height(height), codec(codec), sampleRate(sampleRate) {
    changed_ = true;
}

bool NativeImage::changed() const {
    return changed_;
}

uint32_t* NativeImage::nativePointer() const {
    return data_;
}

void NativeImage::receive(const std::vector<uint8_t>& data) {
    std::lock_guard lock(mutex);
    if (decoder == nullptr) {
        switch (codec) {
            case 0:
                decoder = std::make_unique<VideoDecoderWebP>(width, height);
                break;
            case 1:
                decoder = std::make_unique<VideoDecoderH264>(width, height);
                break;
            case 2:
                decoder = std::make_unique<VideoDecoderAV1>(width, height);
                break;
            default:
                return;
        }
    }
    const auto decoded = decoder->decodeFrame(data);\
    memcpy(data_, decoded.data(), sizeof(uint32_t) * width * height);
    changed_ = true;
}

void NativeImage::receiveAudio(const std::vector<uint8_t> &data) {
    std::lock_guard lock(mutex);
    if (audioDecoder == nullptr) {
        audioDecoder = std::make_unique<AudioDecoderOpus>(48000, 2);
    }
    lastAudioBuffer_ = audioDecoder->decodeFrame(data);
}
