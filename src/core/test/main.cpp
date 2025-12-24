//
// Created by limo on 3/1/25.
//

#include <iostream>
#include <ostream>
#include <codec/VideoDecoder.hpp>
#include <codec/VideoEncoder.hpp>

#include "codec/AudioDecoder.hpp"
#include "codec/AudioEncoder.hpp"
#include "connection/NetworkDefinitions.hpp"

#include <cmath>
#include <cstring>
#include <fstream>

#define log(msg) std::cout << "[Test] " << msg << std::endl

/*std::mutex mutex = {};
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
    for (int i = 0; i < 50; ++i) {
        std::thread([] {
            test_video();
        }).detach();
    }
    std::this_thread::sleep_for(std::chrono::seconds(2));
    std::cout << std::endl << std::endl << std::dec << success << std::endl;
}*/

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
        VideoEncoderH264 encoder(width, height);
        VideoDecoderH264 decoder(width, height);

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

            auto encoded_frame = encoder.encodeFrameRGB565(frame);
            encoded_frames.push_back(encoded_frame);

            total_size += encoded_frame.size();

            auto decoded_frame = decoder.decodeFrame(encoded_frame);
            //decoded_frames.push_back(decoded_frame);
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
    }
    catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
    }
}

void test_audio_codec() {
    try {
        constexpr int sample_rate = 48000;
        constexpr int channels = 2;
        constexpr int frame_duration_ms = 20;  // Recommended Opus frame duration
        constexpr int samples_per_frame = (sample_rate * frame_duration_ms) / 1000 * channels;
        constexpr int count = 1000;

        AudioEncoderOpus encoder(
            48000,
            channels,
            AudioEncoderOpus::Complexity::Balanced
        );
        AudioDecoderOpus decoder(
            48000,
            channels
        );

        std::vector<std::vector<int16_t>> original_frames;
        std::vector<std::vector<uint8_t>> encoded_frames;
        std::vector<std::vector<int16_t>> decoded_frames;

        size_t total_size = 0;

        // Seed random number generator for consistent test
        std::srand(42);

        // Create frames with more robust audio-like data
        for (int i = 0; i < count; ++i) {
            std::vector<int16_t> frame(samples_per_frame);
            for (size_t j = 0; j < frame.size(); ++j) {
                // More complex synthetic audio generation
                const double t = static_cast<double>(j) / sample_rate;

                // Multiple sine waves with different frequencies
                const double sine1 = std::sin(2 * M_PI * 440 * t);   // 440 Hz fundamental
                const double sine2 = std::sin(2 * M_PI * 880 * t);   // First harmonic
                const double sine3 = std::sin(2 * M_PI * 1320 * t); // Second harmonic

                // Add some noise and randomness
                const double noise = (std::rand() / static_cast<double>(RAND_MAX) - 0.5) * 0.1;

                // Combine signals and scale
                const double combined = (sine1 + 0.5 * sine2 + 0.25 * sine3 + noise);

                frame[j] = static_cast<int16_t>(combined * 32767 * 0.5);
            }

            original_frames.push_back(frame);

            try {
                auto encoded_frame = encoder.encodeFrame(frame);
                encoded_frames.push_back(encoded_frame);
                total_size += encoded_frame.size();

                auto decoded_frame = decoder.decodeFrame(encoded_frame);
                decoded_frames.push_back(decoded_frame);
            }
            catch (const std::exception& e) {
                std::cerr << "Encoding/Decoding error on frame " << i << ": " << e.what() << std::endl;
            }
        }

        // More detailed analysis
        size_t matching_frames = 0;
        size_t total_samples_diff = 0;
        size_t max_sample_diff = 0;
        double max_frame_mse = 0.0;

        for (size_t i = 0; i < original_frames.size(); ++i) {
            const auto& orig = original_frames[i];
            const auto& dec = decoded_frames[i];

            if (orig.size() != dec.size()) {
                std::cerr << "Frame size mismatch: " << orig.size() << " vs " << dec.size() << std::endl;
                continue;
            }

            // Calculate Mean Squared Error (MSE) for the frame
            double frame_mse = 0.0;
            size_t frame_sample_diff = 0;
            for (size_t j = 0; j < orig.size(); ++j) {
                int32_t diff = std::abs(static_cast<int32_t>(orig[j]) - static_cast<int32_t>(dec[j]));
                frame_mse += diff * diff;
                frame_sample_diff += diff;
                max_sample_diff = std::max(max_sample_diff, static_cast<size_t>(diff));
            }

            frame_mse /= orig.size();
            frame_mse = std::sqrt(frame_mse);
            max_frame_mse = std::max(max_frame_mse, frame_mse);

            total_samples_diff += frame_sample_diff;

            // More lenient matching for lossy audio codec
            if (frame_mse < 500.0) {  // Adjusted threshold
                matching_frames++;
            }
            else {
                // Detailed logging for problematic frames
                std::cerr << "Frame " << i << " reconstruction error: "
                          << "MSE=" << frame_mse
                          << ", Sample Diff=" << frame_sample_diff / orig.size()
                          << std::endl;
            }
        }

        std::cout << "Audio Codec Test Detailed Results:" << std::endl;
        std::cout << "Frames matched: " << matching_frames
                  << " out of " << original_frames.size()
                  << " (" << (matching_frames * 100.0 / original_frames.size()) << "%)" << std::endl;
        std::cout << "Total encoded size: " << total_size << " bytes" << std::endl;
        std::cout << "Average samples difference: "
                  << static_cast<double>(total_samples_diff) / (count * samples_per_frame)
                  << std::endl;
        std::cout << "Max sample difference: " << max_sample_diff << std::endl;
        std::cout << "Max frame Mean Squared Error: " << max_frame_mse << std::endl;

        encoder.reset();
        decoder.reset();
    }
    catch (const std::exception& e) {
        std::cerr << "Audio Codec Test Critical Error: " << e.what() << std::endl;
    }
}

void manual_codec_test() {
    std::vector<int16_t> testFrame(240 * 160, 0xF800);

    VideoEncoderH264 encoder(240, 160);
    VideoDecoderH264 decoder(240, 160);

    auto data = encoder.encodeFrameRGB565(testFrame);
    auto decoded = decoder.decodeFrame(data);

    auto data2 = encoder.encodeFrameRGB565(testFrame);
    auto decoded2 = decoder.decodeFrame(data);

    auto data3 = encoder.encodeFrameRGB565(testFrame);
    auto decoded3 = decoder.decodeFrame(data);

    bool dataMatchesData2 = memcmp(data.data(), data2.data(), data.size());
    bool data2MatchesData3 = memcmp(data2.data(), data3.data(), data2.size());

    std::cout << "Result 1 matches result 2: " << dataMatchesData2 << std::endl;
    std::cout << "Result 2 matches result 3: " << data2MatchesData3 << std::endl;

    /*std::ofstream out("sussy.h264", std::ios::binary | std::ios::app);
    out.write(reinterpret_cast<const std::ostream::char_type *>(data.data()), data.size());
    out.close();*/
}

int main() {
    manual_codec_test();
    //test_new_codec();
    //test_audio_codec();
    return 0;
}
