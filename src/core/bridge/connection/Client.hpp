//
// Created by limo on 3/8/25.
//

#pragma once
#include <jni.h>
#include <unordered_map>
#include <enet/enet.h>

#include "util/AudioSource.hpp"
#include "util/NativeDisplay.hpp"
#include "util/NativeUtil.hpp"

class RetroClient {
    std::mutex enet_mutex;
    ENetHost* client;
    ENetPeer* peer;

    std::mutex mutex;
    std::unordered_map<long, NativeDisplay*> displays;
    std::unordered_map<long, AudioStreamPlayer*> playbacks;

    bool running = false;
    int runningLoops = 0;
    bool authenticated = false;
    const char* token;

    uint64_t bytesIn = 0;
    uint64_t bytesOut = 0;

public:
    RetroClient(const char* ip, int port, const char* token);

    void dispose();

    void registerDisplay(const jUUID* uuid, NativeDisplay* display, int sampleRate);
    void unregisterDisplay(const jUUID* uuid);
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
