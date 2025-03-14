//
// Created by limo on 3/1/25.
//

#include <iostream>
#include <ostream>
#include <codec/VideoDecoder.hpp>
#include <codec/VideoEncoder.hpp>

#include "connection/NetworkDefinitions.hpp"

#define log(msg) std::cout << "[Test] " << msg << std::endl

void test_video() {
    // Try larger dimensions - many encoders have minimum size requirements
    constexpr int width = 16;
    constexpr int height = 16;

    const auto encoder = new VideoEncoderRGB565(width, height);
    log("Encoder created");
    const auto decoder = new VideoDecoderARGB(width, height);
    log("Decoder created");

    // Create proper-sized test data with a pattern
    constexpr int pixelCount = width * height;
    auto* data = new uint16_t[pixelCount];
    auto* res = new uint32_t[pixelCount];

    // Fill with a simple pattern
    for (int i = 0; i < pixelCount; i++) {
        data[i] = (i % 2) ? 0xFFFF : 0;
    }
    log("Test data created");

    // Encode with error checking
    const auto encoded = encoder->encode(data);
    if (encoded.empty()) {
        log("ERROR: Encoding failed or produced no data");
        delete[] data;
        delete[] res;
        delete encoder;
        delete decoder;
        return;
    }
    log("Data encoded successfully: " + std::to_string(encoded.size()) + " bytes");

    // Clean up
    if (bool decodeResult = decoder->decode(encoded, res); !decodeResult) {
        log("ERROR: Decoding failed");
    } else {
        log("Data decoded successfully");
        std::cout << "Sample decoded values: ";
        for (int i = 0; i < std::min(6, pixelCount); i++) {
            std::cout << std::uppercase << std::hex << res[i] << " ";
        }
        std::cout << std::endl;
    }
    delete[] data;
    delete[] res;
    delete encoder;
    delete decoder;
}

int main() {
    test_video();
}
