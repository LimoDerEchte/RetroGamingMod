//
// Created by limo on 3/28/25.
//

#include "AudioDecoder.hpp"

#include <stdexcept>

AudioDecoderOpus::AudioDecoderOpus(
    const int sample_rate,
    const int channels
) :
    sample_rate(sample_rate),
    channels(channels),
    decoder(nullptr)
{
    initializeDecoder();
}

AudioDecoderOpus::~AudioDecoderOpus() {
    if (decoder) {
        opus_decoder_destroy(decoder);
    }
}

AudioDecoderOpus::AudioDecoderOpus(AudioDecoderOpus&& other) noexcept :
    sample_rate(other.sample_rate),
    channels(other.channels),
    decoder(other.decoder),
    output_buffer(std::move(other.output_buffer))
{
    other.decoder = nullptr;
}

AudioDecoderOpus& AudioDecoderOpus::operator=(AudioDecoderOpus&& other) noexcept {
    if (this != &other) {
        if (decoder) {
            opus_decoder_destroy(decoder);
        }
        sample_rate = other.sample_rate;
        channels = other.channels;
        decoder = other.decoder;
        output_buffer = std::move(other.output_buffer);
        other.decoder = nullptr;
    }
    return *this;
}

void AudioDecoderOpus::initializeDecoder() {
    int opus_error;
    decoder = opus_decoder_create(
        sample_rate,
        channels,
        &opus_error
    );
    if (opus_error != OPUS_OK) {
        throw std::runtime_error("Failed to create Opus decoder");
    }
}

std::vector<int16_t> AudioDecoderOpus::decodeFrame(const std::vector<uint8_t>& encoded_data) {
    if (encoded_data.empty()) {
        return {};
    }
    output_buffer.resize(static_cast<size_t>(sample_rate) * channels);
    const int decoded_samples = opus_decode(
        decoder,
        encoded_data.data(),
        static_cast<int>(encoded_data.size()),
        output_buffer.data(),
        static_cast<int>(output_buffer.size() / channels),
        0
    );
    if (decoded_samples < 0) {
        throw std::runtime_error("Opus decoding failed");
    }
    output_buffer.resize(decoded_samples * channels);
    return output_buffer;
}

void AudioDecoderOpus::reset() {
    if (decoder) {
        opus_decoder_destroy(decoder);
    }
    initializeDecoder();
}
