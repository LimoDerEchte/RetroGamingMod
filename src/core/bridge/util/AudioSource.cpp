//
// Created by limo on 3/28/25.
//

#include "AudioSource.hpp"

#include <stdexcept>
#include <iostream>

AudioStreamPlayer::AudioStreamPlayer(int sampleRate, int channels) :
    decoder(sampleRate, channels),
    device(nullptr),
    context(nullptr),
    source(0),
    running(false)
{
    buffers.resize(NUM_BUFFERS);
    initOpenAL();
}

AudioStreamPlayer::~AudioStreamPlayer() {
    stop();
    cleanupOpenAL();
}

AudioStreamPlayer::AudioStreamPlayer(AudioStreamPlayer&& other) noexcept :
    decoder(std::move(other.decoder)),
    device(other.device),
    context(other.context),
    source(other.source),
    buffers(std::move(other.buffers)),
    running(other.running.load()),
    packetQueue(std::move(other.packetQueue))
{
    other.device = nullptr;
    other.context = nullptr;
    other.source = 0;
    other.running = false;

    if (running) {
        other.stop();
        start();
    }
}

AudioStreamPlayer& AudioStreamPlayer::operator=(AudioStreamPlayer&& other) noexcept {
    if (this != &other) {
        stop();
        cleanupOpenAL();

        decoder = std::move(other.decoder);
        device = other.device;
        context = other.context;
        source = other.source;
        buffers = std::move(other.buffers);
        running = other.running.load();
        packetQueue = std::move(other.packetQueue);

        other.device = nullptr;
        other.context = nullptr;
        other.source = 0;
        other.running = false;

        if (running) {
            start();
        }
    }
    return *this;
}

void AudioStreamPlayer::initOpenAL() {
    // Open default device
    device = alcOpenDevice(nullptr);
    if (!device) {
        throw std::runtime_error("Failed to open OpenAL device");
    }

    // Create context
    context = alcCreateContext(device, nullptr);
    if (!context) {
        alcCloseDevice(device);
        throw std::runtime_error("Failed to create OpenAL context");
    }

    // Make context current
    if (!alcMakeContextCurrent(context)) {
        alcDestroyContext(context);
        alcCloseDevice(device);
        throw std::runtime_error("Failed to make OpenAL context current");
    }

    // Generate source
    alGenSources(1, &source);
    if (alGetError() != AL_NO_ERROR) {
        alcMakeContextCurrent(nullptr);
        alcDestroyContext(context);
        alcCloseDevice(device);
        throw std::runtime_error("Failed to generate OpenAL source");
    }

    // Generate buffers
    alGenBuffers(NUM_BUFFERS, buffers.data());
    if (alGetError() != AL_NO_ERROR) {
        alDeleteSources(1, &source);
        alcMakeContextCurrent(nullptr);
        alcDestroyContext(context);
        alcCloseDevice(device);
        throw std::runtime_error("Failed to generate OpenAL buffers");
    }
}

void AudioStreamPlayer::cleanupOpenAL() {
    if (source) {
        alDeleteSources(1, &source);
    }

    if (!buffers.empty()) {
        alDeleteBuffers(static_cast<ALsizei>(buffers.size()), buffers.data());
        buffers.clear();
    }

    if (context) {
        alcMakeContextCurrent(nullptr);
        alcDestroyContext(context);
    }

    if (device) {
        alcCloseDevice(device);
    }
}

void AudioStreamPlayer::receive(const uint8_t* data, size_t size) {
    if (!data || size == 0) {
        return;
    }

    std::vector<uint8_t> packet(data, data + size);

    {
        std::lock_guard<std::mutex> lock(queueMutex);
        packetQueue.push(std::move(packet));
    }

    queueCondition.notify_one();
}

void AudioStreamPlayer::start() {
    if (running) {
        return;
    }

    running = true;
    playbackThread = std::thread(&AudioStreamPlayer::playbackLoop, this);
}

void AudioStreamPlayer::stop() {
    if (!running) {
        return;
    }

    running = false;
    queueCondition.notify_all();

    if (playbackThread.joinable()) {
        playbackThread.join();
    }

    // Stop the source
    alSourceStop(source);

    // Clear any queued buffers
    ALint queued;
    alGetSourcei(source, AL_BUFFERS_QUEUED, &queued);

    ALuint buffer;
    while (queued--) {
        alSourceUnqueueBuffers(source, 1, &buffer);
    }

    // Clear the packet queue
    std::lock_guard<std::mutex> lock(queueMutex);
    std::queue<std::vector<uint8_t>> empty;
    std::swap(packetQueue, empty);
}

bool AudioStreamPlayer::isPlaying() const {
    return running;
}

void AudioStreamPlayer::reset() {
    stop();
    decoder.reset();
    start();
}

void AudioStreamPlayer::playbackLoop() {
    // Initialize the source with some buffers
    bool initialBuffered = false;

    while (running) {
        // Process available packets
        bool processed = processNextPacket();

        if (!initialBuffered && processed) {
            // Start playback after initial buffers are filled
            alSourcePlay(source);
            initialBuffered = true;
        }

        // Check playback state
        ALint state;
        alGetSourcei(source, AL_SOURCE_STATE, &state);

        if (state != AL_PLAYING && initialBuffered) {
            // If stopped for some reason but should be playing, restart
            alSourcePlay(source);
        }

        // Check for processed buffers
        ALint proc;
        alGetSourcei(source, AL_BUFFERS_PROCESSED, &proc);

        while (proc--) {
            ALuint buffer;
            alSourceUnqueueBuffers(source, 1, &buffer);

            // Try to fill the buffer with a new packet
            std::vector<uint8_t> packet;
            {
                std::lock_guard lock(queueMutex);
                if (!packetQueue.empty()) {
                    packet = std::move(packetQueue.front());
                    packetQueue.pop();
                }
            }

            if (!packet.empty()) {
                try {
                    // Decode packet
                    std::vector<int16_t> pcmData = decoder.decodeFrame(packet);
                    if (!pcmData.empty()) {
                        // Queue the buffer
                        const ALenum format = decoder.getChannels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
                        alBufferData(
                            buffer,
                            format,
                            pcmData.data(),
                            static_cast<ALsizei>(pcmData.size() * sizeof(int16_t)),
                            decoder.getSampleRate()
                        );
                        alSourceQueueBuffers(source, 1, &buffer);
                    }
                } catch (const std::exception& e) {
                    std::cerr << "Error processing audio packet: " << e.what() << std::endl;
                }
            }
        }

        // If no packets to process, wait for more
        if (!proc) {
            std::unique_lock<std::mutex> lock(queueMutex);
            if (packetQueue.empty() && running) {
                queueCondition.wait_for(lock, std::chrono::milliseconds(10));
            }
        }
    }
}

bool AudioStreamPlayer::processNextPacket() {
    std::vector<uint8_t> packet;

    {
        std::lock_guard lock(queueMutex);
        if (packetQueue.empty()) {
            return false;
        }

        packet = std::move(packetQueue.front());
        packetQueue.pop();
    }

    try {
        // Decode packet

        if (const std::vector<int16_t> pcmData = decoder.decodeFrame(packet); !pcmData.empty()) {
            queueBuffer(pcmData);
            return true;
        }
    } catch (const std::exception& e) {
        std::cerr << "Error decoding audio packet: " << e.what() << std::endl;
    }

    return false;
}

void AudioStreamPlayer::queueBuffer(const std::vector<int16_t>& pcmData) const {
    // Find a free buffer
    ALint processed;
    alGetSourcei(source, AL_BUFFERS_PROCESSED, &processed);

    ALuint buffer;
    if (processed > 0) {
        // Unqueue a processed buffer
        alSourceUnqueueBuffers(source, 1, &buffer);
    } else {
        // Check if we have any unused buffers from our pool
        static int bufferIndex = 0;

        ALint queued;
        alGetSourcei(source, AL_BUFFERS_QUEUED, &queued);

        if (queued < NUM_BUFFERS) {
            buffer = buffers[bufferIndex];
            bufferIndex = (bufferIndex + 1) % NUM_BUFFERS;
        } else {
            // No free buffers available
            return;
        }
    }

    // Fill and queue the buffer
    const ALenum format = decoder.getChannels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
    alBufferData(
        buffer,
        format,
        pcmData.data(),
        static_cast<ALsizei>(pcmData.size() * sizeof(int16_t)),
        decoder.getSampleRate()
    );

    alSourceQueueBuffers(source, 1, &buffer);
}
