//
// Created by limo on 12/23/25.
//

#pragma once

#include <memory>
#include <mutex>
#include <codec/VideoDecoder.hpp>
#include <codec/AudioDecoder.hpp>

class NativeImage {
    uint32_t* data_;

    bool changed_;
    int width;
    int height;
    int codec;
    int sampleRate;

    std::mutex mutex;
    std::unique_ptr<VideoDecoder> decoder;
    std::unique_ptr<AudioDecoderOpus> audioDecoder;

public:
    std::vector<float> lastAudioBuffer_;

    NativeImage(int width, int height, uint32_t* data, int codec, int sampleRate);

    [[nodiscard]] bool changed() const;
    [[nodiscard]] uint32_t* nativePointer() const;
    void receive(const std::vector<uint8_t>& data);
    void receiveAudio(const std::vector<uint8_t>& data);
};
