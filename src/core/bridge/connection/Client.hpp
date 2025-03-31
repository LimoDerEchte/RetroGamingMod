//
// Created by limo on 3/8/25.
//

#pragma once
#include <unordered_map>
#include <enet/enet.h>

#include "util/AudioSource.hpp"
#include "util/NativeDisplay.hpp"
#include "util/NativeUtil.hpp"

class RetroClient {
    ENetHost* client;
    ENetPeer* peer;

    std::mutex mutex;
    std::unordered_map<long, NativeDisplay*> displays;
    std::unordered_map<long, AudioStreamPlayer*> playbacks;

    bool running = false;
    bool authenticated = false;
    const char* token;

public:
    RetroClient(const char* ip, int port, const char* token);

    void dispose();

    void registerDisplay(const jUUID* uuid, NativeDisplay* display, int sampleRate);
    void unregisterDisplay(const jUUID* uuid);
    void sendControlsUpdate(const jUUID* link, int port, int16_t controls);

    void mainLoop();
    void onConnect() const;
    void onDisconnect();
    void onMessage(const ENetPacket* packet);

    [[nodiscard]] bool isAuthenticated() const {
        return authenticated;
    }
};
