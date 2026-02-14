
#include "AudioEncoder.hpp"

#include <sstream>
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
    opus_encoder_ctl(encoder, OPUS_SET_BITRATE(128000));
    opus_encoder_ctl(encoder, OPUS_SET_COMPLEXITY(static_cast<int>(complexity)));
    opus_encoder_ctl(encoder, OPUS_SET_VBR(0));
    opus_encoder_ctl(encoder, OPUS_SET_INBAND_FEC(1));
    opus_encoder_ctl(encoder, OPUS_SET_PACKET_LOSS_PERC(5));
    if (opus_error != OPUS_OK) {
        std::stringstream error_msg;
        error_msg << "Failed to create Opus encoder: "
                  << "Error code " << opus_error << " ("
                  << getOpusErrorString(opus_error) << ")"
                  << ", Sample rate: " << sample_rate
                  << ", Channels: " << channels;
        throw std::runtime_error(error_msg.str());
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
        std::stringstream error_msg;
        error_msg << "Opus encoding failed: "
                  << "Error code " << encoded_bytes << " ("
                  << getOpusErrorString(encoded_bytes) << ")"
                  << ", Input size: " << pcm_data.size()
                  << " samples, Buffer size: " << output_buffer.size()
                  << " bytes";
        throw std::runtime_error(error_msg.str());
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

std::string AudioEncoderOpus::getOpusErrorString(const int error_code) {
    switch(error_code) {
        case OPUS_OK:
            return "No error";
        case OPUS_BAD_ARG:
            return "One or more invalid/out of range arguments";
        case OPUS_BUFFER_TOO_SMALL:
            return "The buffer is too small";
        case OPUS_INTERNAL_ERROR:
            return "An internal error was detected";
        case OPUS_INVALID_PACKET:
            return "The compressed data passed is corrupted";
        case OPUS_UNIMPLEMENTED:
            return "The requested feature is not implemented";
        case OPUS_INVALID_STATE:
            return "The encoder or decoder is in an invalid state";
        case OPUS_ALLOC_FAIL:
            return "Memory allocation failed";
        default:
            return "Unknown error";
    }
}
