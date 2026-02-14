
#include "AudioDecoder.hpp"

#include <sstream>
#include <stdexcept>
#include <stdfloat>
#include <codec/AudioEncoder.hpp>

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
    decoder(other.decoder)
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
        std::stringstream error_msg;
        error_msg << "Failed to create Opus decoder: "
                  << "Error code " << opus_error << " ("
                  << AudioEncoderOpus::getOpusErrorString(opus_error) << ")"
                  << ", Sample rate: " << sample_rate
                  << ", Channels: " << channels;
        throw std::runtime_error(error_msg.str());
    }
}

std::vector<float> AudioDecoderOpus::decodeFrame(const std::vector<uint8_t>& encoded_data) const {
    if (encoded_data.empty()) {
        return {};
    }
    std::vector<float> output(static_cast<size_t>(sample_rate) * channels);

    const int decoded_samples = opus_decode_float(
        decoder,
        encoded_data.data(),
        static_cast<int>(encoded_data.size()),
        output.data(),
        static_cast<int>(output.size() / channels),
        0
    );
    if (decoded_samples < 0) {
        std::stringstream error_msg;
        error_msg << "Opus decoding failed: "
                  << "Error code " << decoded_samples << " ("
                  << AudioEncoderOpus::getOpusErrorString(decoded_samples) << ")"
                  << ", Input size: " << encoded_data.size()
                  << " bytes, Buffer size: " << output.size()
                  << " samples";
        throw std::runtime_error(error_msg.str());
    }
    output.resize(decoded_samples * channels);
    return output;
}

void AudioDecoderOpus::reset() {
    if (decoder) {
        opus_decoder_destroy(decoder);
    }
    initializeDecoder();
}
