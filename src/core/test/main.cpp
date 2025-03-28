//
// Created by limo on 3/1/25.
//

#include <iostream>
#include <ostream>
#include <thread>
#include <codec/VideoDecoder.hpp>
#include <codec/VideoEncoder.hpp>

#include "connection/NetworkDefinitions.hpp"
#include "new_codec/VideoDecoder.hpp"
#include "new_codec/VideoEncoder.hpp"

#define log(msg) std::cout << "[Test] " << msg << std::endl

std::mutex mutex = {};
int success = 0;

void test_video() {
    // Try larger dimensions - many encoders have minimum size requirements
    constexpr int width = 160;
    constexpr int height = 144;

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
        if (encoded->empty()) {
            log("ERROR: Encoding failed or produced no data");
            delete[] data;
            delete[] res;
            delete encoder;
            delete decoder;
            return;
        }
        log("Data encoded successfully: " + std::to_string(encoded->size()) + " bytes");

        const auto packet = Int8ArrayPacket(PACKET_UPDATE_DISPLAY, new jUUID{0,0}, encoded->data(), encoded->size()).pack();
        log("Data encoded packet: " + std::to_string(packet->dataLength) + " bytes");

        const auto unpacked = Int8ArrayPacket::unpack(packet);

        // Clean up
        if (const bool decodeResult = decoder->decode(std::vector(unpacked->data, unpacked->data + unpacked->size), res); !decodeResult) {
            log("ERROR: Decoding failed");
        } else {
            log("Data decoded successfully");
            /*std::cout << "Sample decoded values: ";
            for (int i2 = 0; i2 < pixelCount; i2++) {
                std::cout << std::uppercase << std::hex << res[i2] << " ";
            }
            std::cout << std::endl;
            */
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
    for (int i = 0; i < 50; ++i) {
        std::thread([] {
            test_video();
        }).detach();
    }
    std::this_thread::sleep_for(std::chrono::seconds(2));
    std::cout << std::endl << std::endl << std::dec << success << std::endl;
}

bool compareFrames(const std::vector<int16_t>& original, const std::vector<int16_t>& decoded) {
    if (original.size() != decoded.size()) {
        return false;
    }

    for (size_t i = 0; i < original.size(); ++i) {
        if (std::abs(original[i] - decoded[i]) > 1) {
            std::cout << "Mismatch at index " << i
                      << ": original=" << original[i]
                      << ", decoded=" << decoded[i] << std::endl;
            return false;
        }
    }
    return true;
}

void test_new_codec() {
    try {
        constexpr int width = 240;
        constexpr int height = 160;
        constexpr int count = 1000;
        VideoEncoder encoder(width, height);
        VideoDecoder decoder(width, height);

        std::vector<std::vector<int16_t>> original_frames;
        std::vector<std::vector<uint8_t>> encoded_frames;
        std::vector<std::vector<int16_t>> decoded_frames;

        size_t total_size = 0;
        for (int i = 1; i <= count; ++i) {
            std::vector<int16_t> frame(width * height);
            for (size_t j = 0; j < frame.size(); ++j) {
                frame[j] = static_cast<int16_t>(100 + i * 2 + (j % i));
            }
            original_frames.push_back(frame);

            auto encoded_frame = encoder.encodeFrame(frame);
            encoded_frames.push_back(encoded_frame);

            total_size += encoded_frame.size();

            auto decoded_frame = decoder.decodeFrame(encoded_frame);
            decoded_frames.push_back(decoded_frame);
        }

        size_t matching_frames = 0;
        for (size_t i = 0; i < original_frames.size(); ++i) {
            if (compareFrames(original_frames[i], decoded_frames[i])) {
                matching_frames++;
            }
        }

        std::cout << "Frames matched: " << matching_frames << " out of " << original_frames.size() << std::endl;
        std::cout << "Total size: " << total_size << std::endl;
        const size_t speed = total_size * 30 / count;
        std::cout << "Speed: " << speed << std::endl;

        encoder.reset();
        decoder.reset();
    }
    catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
    }
}

int main() {
    test_new_codec();
    return 0;
}
