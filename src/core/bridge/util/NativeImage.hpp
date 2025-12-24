//
// Created by limo on 12/23/25.
//

#pragma once

#include <cstdint>
#include <memory>
#include <mutex>
#include <codec/VideoDecoder.hpp>

class NativeImage {
    uint32_t* data_;
    bool changed_;
    int width_;
    int height_;
    int codec_;

    std::mutex mutex_;
    std::unique_ptr<VideoDecoderH264> decoder_;

public:
    NativeImage(int width, int height, uint32_t* data, int codec);

    [[nodiscard]] bool changed() const;
    [[nodiscard]] uint32_t* nativePointer() const;
    void receive(const std::vector<uint8_t>& data);
};
