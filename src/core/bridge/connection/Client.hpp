//
// Created by limo on 3/8/25.
//

#pragma once
#include <unordered_map>
#include <enet/enet.h>

#include "util/NativeDisplay.hpp"
#include "util/NativeUtil.hpp"

class RetroClient {
    ENetHost* client;
    ENetPeer* peer;
    std::unordered_map<long, const NativeDisplay*> displays;
    bool running = false;
    bool authenticated = false;
    const char* token;

public:
    RetroClient(const char* ip, int port, const char* token);

    void dispose();

    void registerDisplay(const jUUID* uuid, const NativeDisplay* display);
    void unregisterDisplay(const jUUID* uuid);

    void mainLoop();
    void onConnect() const;
    void onDisconnect();
    void onMessage(const ENetPacket* packet);

    [[nodiscard]] bool isAuthenticated() const {
        return authenticated;
    }
};
