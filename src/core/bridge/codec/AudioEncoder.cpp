//
// Created by limo on 3/28/25.
//

#include "AudioEncoder.hpp"

#include <stdexcept>

AudioEncoderOpus::AudioEncoderOpus(
    const int sample_rate,
    const int channels,
    const Complexity complexity
) :
    sample_rate(sample_rate),
    channels(channels),
    complexity(complexity),
    encoder(nullptr)
{
    initializeEncoder();
}

AudioEncoderOpus::~AudioEncoderOpus() {
    if (encoder) {
        opus_encoder_destroy(encoder);
    }
}

AudioEncoderOpus::AudioEncoderOpus(AudioEncoderOpus&& other) noexcept :
    sample_rate(other.sample_rate),
    channels(other.channels),
    complexity(other.complexity),
    encoder(other.encoder),
    output_buffer(std::move(other.output_buffer))
{
    other.encoder = nullptr;
}

AudioEncoderOpus& AudioEncoderOpus::operator=(AudioEncoderOpus&& other) noexcept {
    if (this != &other) {
        if (encoder) {
            opus_encoder_destroy(encoder);
        }
        sample_rate = other.sample_rate;
        channels = other.channels;
        complexity = other.complexity;
        encoder = other.encoder;
        output_buffer = std::move(other.output_buffer);
        other.encoder = nullptr;
    }
    return *this;
}

void AudioEncoderOpus::initializeEncoder() {
    int opus_error;
    encoder = opus_encoder_create(
        sample_rate,
        channels,
        OPUS_APPLICATION_AUDIO,
        &opus_error
    );
    if (opus_error != OPUS_OK) {
        throw std::runtime_error("Failed to create Opus encoder");
    }
    opus_encoder_ctl(encoder, OPUS_SET_COMPLEXITY(static_cast<int>(complexity)));
}

std::vector<uint8_t> AudioEncoderOpus::encodeFrame(const std::vector<int16_t>& pcm_data) {
    if (pcm_data.empty()) {
        return {};
    }
    output_buffer.resize(pcm_data.size());
    const int encoded_bytes = opus_encode(
        encoder,
        pcm_data.data(),
        static_cast<int>(pcm_data.size() / channels),
        output_buffer.data(),
        static_cast<int>(output_buffer.size())
    );
    if (encoded_bytes < 0) {
        throw std::runtime_error("Opus encoding failed");
    }
    output_buffer.resize(encoded_bytes);
    return output_buffer;
}

void AudioEncoderOpus::reset() {
    if (encoder) {
        opus_encoder_destroy(encoder);
    }
    initializeEncoder();
}
