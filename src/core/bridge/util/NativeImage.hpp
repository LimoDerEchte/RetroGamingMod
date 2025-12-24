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

    std::mutex mutex_;
    std::unique_ptr<VideoDecoderInt16> decoder_;

public:
    NativeImage(int width, int height);

    [[nodiscard]] bool changed() const;
    [[nodiscard]] uint32_t* nativePointer() const;
    void receive(const std::vector<uint8_t>& data);
};
