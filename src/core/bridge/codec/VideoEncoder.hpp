//
// Created by limo on 3/13/25.
//

#pragma once
#include <cstdint>
#include <vector>

class VideoEncoderInt16 {
    static constexpr int RAW_FRAME_INTERVAL = 30;

    const int width, height;
    int frame_count = 0;
    std::vector<int16_t> previous_frame;

    std::vector<int16_t> performDeltaEncoding(const std::vector<int16_t>& current_frame);
    static std::vector<uint8_t> compressWithZlib(const std::vector<int16_t>& data, bool is_raw_frame);

public:
    VideoEncoderInt16(int width, int height);

    std::vector<uint8_t> encodeFrame(const std::vector<int16_t>& frame);
    void reset();

    [[nodiscard]] int getWidth() const;
    [[nodiscard]] int getHeight() const;
};
