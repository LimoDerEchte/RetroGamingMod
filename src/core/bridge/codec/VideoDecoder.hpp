//
// Created by limo on 3/13/25.
//

#pragma once
#include <cstdint>
#include <vector>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}

class VideoDecoderInt16 {
    const int width, height;
    std::vector<int16_t> previous_frame;

    std::vector<int16_t> performInverseDeltaEncoding(const std::vector<int16_t>& delta_frame);
    [[nodiscard]] std::vector<int16_t> decompressWithZlib(const std::vector<uint8_t>& compressed_data) const;

public:
    VideoDecoderInt16(int width, int height);

    std::vector<int16_t> decodeFrame(const std::vector<uint8_t>& encoded_data);
    void reset();

    [[nodiscard]] int getWidth() const;
    [[nodiscard]] int getHeight() const;
};