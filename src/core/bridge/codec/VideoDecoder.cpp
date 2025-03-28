//
// Created by limo on 3/13/25.
//

#include "VideoDecoder.hpp"

#include <stdexcept>
#include <zlib.h>

std::vector<int16_t> VideoDecoderInt16::performInverseDeltaEncoding(const std::vector<int16_t> &delta_frame) {
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

std::vector<int16_t> VideoDecoderInt16::decompressWithZlib(const std::vector<uint8_t> &compressed_data) const {
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
    memcpy(decompressed_frame.data(), decompressed_buffer.data(), decompressed_size);
    return decompressed_frame;
}

VideoDecoderInt16::VideoDecoderInt16(const int width, const int height) : width(width), height(height) {
}

std::vector<int16_t> VideoDecoderInt16::decodeFrame(const std::vector<uint8_t> &encoded_data) {
    std::vector<int16_t> decompressed_frame = decompressWithZlib(encoded_data);
    if (encoded_data[0] == 1) {
        previous_frame = decompressed_frame;
        return decompressed_frame;
    }
    return performInverseDeltaEncoding(decompressed_frame);
}

void VideoDecoderInt16::reset() {
    previous_frame.clear();
}

int VideoDecoderInt16::getWidth() const {
    return width;
}

int VideoDecoderInt16::getHeight() const {
    return height;
}
