//
// Created by limo on 3/1/25.
//

#include <iostream>
#include <ostream>
#include <thread>
#include <codec/VideoDecoder.hpp>
#include <codec/VideoEncoder.hpp>

#include "connection/NetworkDefinitions.hpp"

#define log(msg) std::cout << "[Test] " << msg << std::endl

std::mutex mutex = {};
int success = 0;

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

    for (int i = 0; i < 5; i++) {
        // Fill with a simple pattern
        for (int i1 = 0; i1 < pixelCount; i1++) {
            data[i1] = (i1 % 2) ? 0xFFFF : 0;
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

        const auto packet = Int8ArrayPacket(PACKET_UPDATE_DISPLAY, new jUUID{0,0}, encoded.data(), encoded.size()).pack();
        log("Data encoded packet: " + std::to_string(packet->dataLength) + " bytes");

        const auto unpacked = Int8ArrayPacket::unpack(packet);

        // Clean up
        if (const bool decodeResult = decoder->decode(std::vector(unpacked->data, unpacked->data + unpacked->size), res); !decodeResult) {
            log("ERROR: Decoding failed");
        } else {
            log("Data decoded successfully");
            std::cout << "Sample decoded values: ";
            for (int i2 = 0; i2 < pixelCount; i2++) {
                std::cout << std::uppercase << std::hex << res[i2] << " ";
            }
            std::cout << std::endl;

            mutex.lock();
            success++;
            mutex.unlock();
        }
    }
    delete[] data;
    delete[] res;
    delete encoder;
    delete decoder;
}

void test_video_multithread() {
    for (int i = 0; i < 10; ++i) {
        std::thread([] {
            test_video();
        }).detach();
    }
    std::this_thread::sleep_for(std::chrono::seconds(2));
    std::cout << std::endl << std::endl << success << std::endl;
}

int main() {
    test_video_multithread();
}
