//
// Created by limo on 3/28/25.
//

#include "AudioSource.hpp"

#include <stdexcept>
#include <iostream>

AudioStreamPlayer::AudioStreamPlayer(const int sampleRate, const int channels) :
    decoder(48000, channels),
    device(nullptr),
    context(nullptr),
    source(0),
    sampleRate(sampleRate),
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
    sampleRate(other.sampleRate),
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

void AudioStreamPlayer::receive(const std::vector<uint8_t> &data) {
    std::lock_guard lock(queueMutex);
    packetQueue.push(data);
    queueCondition.notify_one();
}

void AudioStreamPlayer::initOpenAL() {
    std::lock_guard al_lock(al_mutex);
    device = alcOpenDevice(nullptr);
    if (!device) {
        throw std::runtime_error("Failed to open OpenAL device");
    }
    context = alcCreateContext(device, nullptr);
    if (!context) {
        alcCloseDevice(device);
        throw std::runtime_error("Failed to create OpenAL context");
    }
    if (!alcMakeContextCurrent(context)) {
        alcDestroyContext(context);
        alcCloseDevice(device);
        throw std::runtime_error("Failed to make OpenAL context current");
    }
    alGenSources(1, &source);
    if (alGetError() != AL_NO_ERROR) {
        alcMakeContextCurrent(nullptr);
        alcDestroyContext(context);
        alcCloseDevice(device);
        throw std::runtime_error("Failed to generate OpenAL source");
    }
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
    std::lock_guard al_lock(al_mutex);
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

void AudioStreamPlayer::updateDistance(const double distance) {
    std::lock_guard al_lock(al_mutex);
    alSourcef(source, AL_GAIN, static_cast<float>(std::max(1.5, 1.5 / distance * .2)));
}

void AudioStreamPlayer::start() {
    if (running) {
        return;
    }
    running = true;
    playbackThread = std::thread(&AudioStreamPlayer::playbackLoop, this);
}

void AudioStreamPlayer::stop() {
    {
        std::lock_guard al_lock(al_mutex);
        if (!running)
            return;
        running = false;
    }

    queueCondition.notify_all();
    if (playbackThread.joinable()) {
        playbackThread.join();
    }

    {
        std::lock_guard al_lock(al_mutex);
        alSourceStop(source);
        ALint queued;
        alGetSourcei(source, AL_BUFFERS_QUEUED, &queued);
        ALuint buffer;
        while (queued--) {
            alSourceUnqueueBuffers(source, 1, &buffer);
        }
    }
    std::lock_guard lock(queueMutex);
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
    bool initialBuffered = false;
    while (running) {
        al_mutex.lock();
        if (const bool processed = processNextPacket(); !initialBuffered && processed) {
            alSourcePlay(source);
            initialBuffered = true;
        }
        ALint state;
        alGetSourcei(source, AL_SOURCE_STATE, &state);
        if (state != AL_PLAYING && initialBuffered) {
            alSourcePlay(source);
        }
        ALint proc;
        alGetSourcei(source, AL_BUFFERS_PROCESSED, &proc);
        while (proc--) {
            ALuint buffer;
            alSourceUnqueueBuffers(source, 1, &buffer);
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
                    std::vector<int16_t> pcmData = decoder.decodeFrame(packet);
                    if (!pcmData.empty()) {
                        const ALenum format = decoder.getChannels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
                        alBufferData(
                            buffer,
                            format,
                            pcmData.data(),
                            static_cast<ALsizei>(pcmData.size() * sizeof(int16_t)),
                            sampleRate
                        );
                        alSourceQueueBuffers(source, 1, &buffer);
                    }
                } catch (const std::exception& e) {
                    std::cerr << "Error processing audio packet: " << e.what() << std::endl;
                }
            }
        }
        al_mutex.unlock();
        if (!proc) {
            std::unique_lock lock(queueMutex);
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
    ALint processed;
    alGetSourcei(source, AL_BUFFERS_PROCESSED, &processed);
    ALuint buffer;
    if (processed > 0) {
        alSourceUnqueueBuffers(source, 1, &buffer);
    } else {
        static int bufferIndex = 0;

        ALint queued;
        alGetSourcei(source, AL_BUFFERS_QUEUED, &queued);

        if (queued < NUM_BUFFERS) {
            buffer = buffers[bufferIndex];
            bufferIndex = (bufferIndex + 1) % NUM_BUFFERS;
        } else {
            return;
        }
    }
    const ALenum format = decoder.getChannels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
    alBufferData(
        buffer,
        format,
        pcmData.data(),
        static_cast<ALsizei>(pcmData.size() * sizeof(int16_t)),
        sampleRate
    );
    alSourceQueueBuffers(source, 1, &buffer);
}
