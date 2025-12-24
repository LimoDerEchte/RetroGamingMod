//
// Created by limo on 3/13/25.
//

#pragma once

#include <cstdint>
#include <vector>
#include <wels/codec_api.h>

class VideoDecoderH264 {
    const int width, height;
    ISVCDecoder* decoder;

public:
    VideoDecoderH264(int width, int height);
    ~VideoDecoderH264();

    [[nodiscard]] std::vector<int32_t> decodeFrame(const std::vector<uint8_t>& encoded_data) const;
    [[nodiscard]] int getWidth() const;
    [[nodiscard]] int getHeight() const;
};