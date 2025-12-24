//
// Created by limo on 3/13/25.
//

#pragma once

#include <cstdint>
#include <vector>
#include <wels/codec_api.h>

class VideoEncoderInt16 {
    static constexpr int RAW_FRAME_INTERVAL = 30;

    const int width, height;
    ISVCEncoder* encoder;

public:
    VideoEncoderInt16(int width, int height);
    ~VideoEncoderInt16();

    [[nodiscard]] std::vector<uint8_t> encodeFrame(const std::vector<int16_t>& frame) const;

    [[nodiscard]] int getWidth() const;
    [[nodiscard]] int getHeight() const;
};
