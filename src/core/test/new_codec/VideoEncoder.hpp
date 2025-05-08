//
// Created by limo on 3/28/25.
//

#pragma once

#include <vector>
#include <cstdint>
#include <zlib.h>
#include <stdexcept>
#include <cstring>

class VideoEncoder {
    int width;
    int height;

    static constexpr int RAW_FRAME_INTERVAL = 30;

    int frame_count = 0;

    std::vector<int16_t> previous_frame;

    std::vector<int16_t> performDeltaEncoding(const std::vector<int16_t>& current_frame) {
        std::vector<int16_t> delta_encoded(current_frame.size());

        if (previous_frame.empty()) {
            delta_encoded = current_frame;
        } else {
            int16_t prevVal = 0;
            for (size_t i = 0; i < current_frame.size(); ++i) {
                const auto frame_delta = static_cast<int16_t>(current_frame[i] - previous_frame[i]);
                const auto packet_delta = static_cast<int16_t>(prevVal - frame_delta);
                delta_encoded[i] = packet_delta;
                prevVal = frame_delta;
            }
        }
        previous_frame = current_frame;
        return delta_encoded;
    }

    static std::vector<uint8_t> compressWithZlib(const std::vector<int16_t>& data, const bool is_raw_frame) {
        std::vector<uint8_t> input_buffer(data.size() * sizeof(int16_t));
        std::memcpy(input_buffer.data(), data.data(), input_buffer.size());

        uLongf compressed_size = compressBound(input_buffer.size());
        std::vector<uint8_t> compressed_buffer(compressed_size + 1);

        const int zlib_result = compress2(
            compressed_buffer.data() + 1,
            &compressed_size,
            input_buffer.data(),
            input_buffer.size(),
            Z_BEST_COMPRESSION
        );

        if (zlib_result != Z_OK) {
            throw std::runtime_error("Compression failed");
        }

        compressed_buffer[0] = is_raw_frame ? 1 : 0;
        compressed_buffer.resize(compressed_size + 1);
        return compressed_buffer;
    }

public:
    VideoEncoder(const int frame_width, const int frame_height)
        : width(frame_width), height(frame_height) {}

    std::vector<uint8_t> encodeFrame(const std::vector<int16_t>& frame) {
        if (frame.size() != width * height) {
            throw std::invalid_argument("Frame size does not match encoder dimensions");
        }
        frame_count++;
        if (frame_count % RAW_FRAME_INTERVAL == 0) {
            previous_frame = frame;
            return compressWithZlib(frame, true);
        }
        const std::vector<int16_t> delta_encoded = performDeltaEncoding(frame);
        return compressWithZlib(delta_encoded, false);
    }

    void reset() {
        previous_frame.clear();
        frame_count = 0;
    }

    [[nodiscard]] int getWidth() const {
        return width;
    }

    [[nodiscard]] int getHeight() const {
        return height;
    }
};
