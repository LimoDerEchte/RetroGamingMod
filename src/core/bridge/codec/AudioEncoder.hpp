
#pragma once

#include <cstdint>
#include <vector>
#include <memory>

#ifdef _WIN32
#include <string>
#endif

#if __has_include(<libopus/opus.h>)
    #include <libopus/opus.h>
#elif __has_include(<opus/opus.h>)
    #include <opus/opus.h>
#elif __has_include(<opus.h>)
    #include <opus.h>
#else
    #error "Opus header not found. Please install libopus-dev or specify the correct include path."
#endif

class AudioEncoderOpus {
public:
    enum class Complexity {
        Fast = 0,
        Balanced = 5,
        Quality = 10
    };

private:
    int sample_rate;
    int channels;
    Complexity complexity;
    OpusEncoder* encoder;
    std::vector<uint8_t> output_buffer;

    void initializeEncoder();

public:
    explicit AudioEncoderOpus(
        int sample_rate = 48000,
        int channels = 2,
        Complexity complexity = Complexity::Balanced
    );
    ~AudioEncoderOpus();

    AudioEncoderOpus(const AudioEncoderOpus&) = delete;
    AudioEncoderOpus& operator=(const AudioEncoderOpus&) = delete;

    AudioEncoderOpus(AudioEncoderOpus&& other) noexcept;
    AudioEncoderOpus& operator=(AudioEncoderOpus&& other) noexcept;

    std::vector<uint8_t> encodeFrame(const std::vector<int16_t>& pcm_data);
    void reset();

    static std::string getOpusErrorString(int error_code);

    [[nodiscard]] int getSampleRate() const {
        return sample_rate;
    }

    [[nodiscard]] int getChannels() const {
        return channels;
    }
};
