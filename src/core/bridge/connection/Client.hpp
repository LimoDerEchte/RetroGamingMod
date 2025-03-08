//
// Created by limo on 3/8/25.
//

#pragma once
#include <enet/enet.h>

class RetroClient {
    ENetHost* client;
    ENetPeer* peer;
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
