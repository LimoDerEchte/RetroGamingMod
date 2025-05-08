//
// Created by limo on 3/28/25.
//

#pragma once

#include <vector>
#include <cstdint>
#include <algorithm>
#include <zlib.h>
#include <stdexcept>
#include <cstring>

class VideoDecoder {
    int width;
    int height;

    std::vector<int16_t> previous_frame;

    std::vector<int16_t> performInverseDeltaEncoding(const std::vector<int16_t>& delta_frame) {
        std::vector<int16_t> decoded_frame(delta_frame.size());

        if (previous_frame.empty()) {
            decoded_frame = delta_frame;
        } else {
            int16_t prevVal = 0;
            for (size_t i = 0; i < delta_frame.size(); ++i) {
                const auto packet_delta = static_cast<int16_t>(prevVal - delta_frame[i]);
                const auto frame_delta = static_cast<int16_t>(previous_frame[i] + packet_delta);
                decoded_frame[i] = frame_delta;
                prevVal = packet_delta;
            }
        }
        previous_frame = decoded_frame;
        return decoded_frame;
    }

    [[nodiscard]] std::vector<int16_t> decompressWithZlib(const std::vector<uint8_t>& compressed_data) const {
        uLongf decompressed_size = width * height * sizeof(int16_t);
        std::vector<uint8_t> decompressed_buffer(decompressed_size);

        const int zlib_result = uncompress(
            decompressed_buffer.data(),
            &decompressed_size,
            compressed_data.data() + 1,
            compressed_data.size() - 1
        );

        if (zlib_result != Z_OK) {
            throw std::runtime_error("Decompression failed");
        }

        std::vector<int16_t> decompressed_frame(width * height);
        std::memcpy(decompressed_frame.data(), decompressed_buffer.data(), decompressed_size);
        return decompressed_frame;
    }

public:
    VideoDecoder(const int frame_width, const int frame_height)
        : width(frame_width), height(frame_height) {}

    std::vector<int16_t> decodeFrame(const std::vector<uint8_t>& encoded_data) {
        const bool is_raw_frame = encoded_data[0] == 1;

        std::vector<int16_t> decompressed_frame = decompressWithZlib(encoded_data);

        if (is_raw_frame) {
            previous_frame = decompressed_frame;
            return decompressed_frame;
        }

        return performInverseDeltaEncoding(decompressed_frame);
    }

    void reset() {
        previous_frame.clear();
    }

    [[nodiscard]] int getWidth() const {
        return width;
    }

    [[nodiscard]] int getHeight() const {
        return height;
    }
};
