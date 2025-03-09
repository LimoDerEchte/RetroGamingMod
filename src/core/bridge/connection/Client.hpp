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
    std::unordered_map<jUUID, NativeDisplay*> displays;
    bool running = false;
    bool authenticated = false;

public:
    RetroClient(const char* ip, int port, const char* token);

    void dispose();

    void mainLoop();
    void onConnect() const;
    void onDisconnect();
    void onMessage(const ENetPacket* packet);

    [[nodiscard]] bool isAuthenticated() const {
        return authenticated;
    }
};
