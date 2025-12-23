//
// Created by limo on 3/8/25.
//

#pragma once
#include <shared_mutex>
#include <unordered_map>
#include <enet/enet.h>

#include "util/AudioSource.hpp"
#include "util/NativeImage.hpp"
#include "util/NativeUtil.hpp"

class RetroClient {
    std::shared_mutex enetMutex;
    ENetHost* client;
    ENetPeer* peer;

    std::shared_mutex mapMutex;
    std::unordered_map<long, std::shared_ptr<NativeImage>> displays;
    std::unordered_map<long, std::shared_ptr<AudioStreamPlayer>> playbacks;

    bool running = false;
    int runningLoops = 0;
    bool authenticated = false;
    const char* token;

    uint64_t bytesIn = 0;
    uint64_t bytesOut = 0;

public:
    RetroClient(const char* ip, int port, const char* token);

    void dispose();

    uint32_t* registerDisplay(const jUUID* uuid, int width, int height, int sampleRate);
    void unregisterDisplay(const jUUID* uuid);
    std::shared_ptr<NativeImage> getDisplay(const jUUID* uuid);
    void sendControlsUpdate(const jUUID* link, int port, int16_t controls);
    void updateAudioDistance(const jUUID* uuid, double distance);

    void mainLoop();
    void bandwidthMonitorLoop();
    void onConnect();
    void onDisconnect();
    void onMessage(const ENetPacket* packet);

    [[nodiscard]] bool isAuthenticated() const {
        return authenticated;
    }
};
