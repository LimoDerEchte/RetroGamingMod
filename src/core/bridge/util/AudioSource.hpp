//
// Created by limo on 3/28/25.
//

#pragma once
#include <AL/al.h>
#include <AL/alc.h>
#include <queue>
#include <mutex>
#include <thread>
#include <atomic>
#include <condition_variable>
#include <jni.h>

#include "codec/AudioDecoder.hpp"

class AudioStreamPlayer {
    AudioDecoderOpus decoder;

    ALCdevice* device;
    ALCcontext* context;
    ALuint source;
    std::vector<ALuint> buffers;
    const int sampleRate;
    static constexpr int NUM_BUFFERS = 8;
    static constexpr int MIN_BUFFERS_TO_START = 3;

    std::thread playbackThread;
    std::atomic<bool> running;
    std::mutex al_mutex;
    std::mutex queueMutex;
    std::condition_variable queueCondition;
    std::queue<std::vector<uint8_t>> packetQueue;

    void initOpenAL();
    void cleanupOpenAL();
    void playbackLoop();
    bool processNextPacket();
    void queueBuffer(const std::vector<int16_t>& pcmData) const;

public:
    explicit AudioStreamPlayer(int sampleRate = 48000, int channels = 2);
    ~AudioStreamPlayer();

    AudioStreamPlayer(const AudioStreamPlayer&) = delete;
    AudioStreamPlayer& operator=(const AudioStreamPlayer&) = delete;

    AudioStreamPlayer(AudioStreamPlayer&& other) noexcept;
    AudioStreamPlayer& operator=(AudioStreamPlayer&& other) noexcept;

    void receive(const uint8_t* data, size_t size);
    void updateDistance(double distance);
    void start();
    void stop();
    [[nodiscard]] bool isPlaying() const;

    void reset();
};
