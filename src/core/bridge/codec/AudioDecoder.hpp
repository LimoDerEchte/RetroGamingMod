//
// Created by limo on 3/28/25.
//

#pragma once
#include <cstdint>
#include <vector>
#include <memory>

#if __has_include(<libopus/opus.h>)
    #include <libopus/opus.h>
#elif __has_include(<opus/opus.h>)
    #include <opus/opus.h>
#elif __has_include(<opus.h>)
    #include <opus.h>
#else
    #error "Opus header not found. Please install libopus-dev or specify the correct include path."
#endif

class AudioDecoderOpus {
    int sample_rate;
    int channels;
    OpusDecoder* decoder;
    std::vector<int16_t> output_buffer;

    void initializeDecoder();

public:
    explicit AudioDecoderOpus(
        int sample_rate = 48000,
        int channels = 2
    );
    ~AudioDecoderOpus();

    AudioDecoderOpus(const AudioDecoderOpus&) = delete;
    AudioDecoderOpus& operator=(const AudioDecoderOpus&) = delete;

    AudioDecoderOpus(AudioDecoderOpus&& other) noexcept;
    AudioDecoderOpus& operator=(AudioDecoderOpus&& other) noexcept;

    std::vector<int16_t> decodeFrame(const std::vector<uint8_t>& encoded_data);
    void reset();

    [[nodiscard]] int getSampleRate() const {
        return sample_rate;
    }

    [[nodiscard]] int getChannels() const {
        return channels;
    }
};
