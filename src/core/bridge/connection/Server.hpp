//
// Created by limo on 3/8/25.
//

#pragma once
#include <vector>
#include <enet/enet.h>

#include "NetworkDefinitions.hpp"

struct RetroServerClient {
    ENetPeer* peer;
    bool isAuthenticated = false;

    explicit RetroServerClient(ENetPeer* peer) : peer(peer) {}
};

class RetroServer {
    ENetHost* server;
    bool running = false;
    std::vector<RetroServerClient*>* clients = new std::vector<RetroServerClient*>(SERVER_MAX_CLIENTS);
    std::vector<std::array<char, 32>>* tokens = new std::vector<std::array<char, 32>>();

public:
    explicit RetroServer(int port);

    [[nodiscard]] std::array<char, 32> genToken() const;
    void dispose();

    void mainLoop() const;
    void onConnect(ENetPeer* peer) const;
    void onDisconnect(ENetPeer* peer) const;
    void onMessage(ENetPeer* peer, const ENetPacket* packet) const;

    static void kick(ENetPeer *peer, const char *message);
    RetroServerClient* findClientByPeer(const ENetPeer* peer) const;
};

inline RetroServerClient* RetroServer::findClientByPeer(const ENetPeer* peer) const {
    for (const auto element : *clients) {
        if (element->peer == peer) {
            return element;
        }
    }
    return nullptr;
}

inline void RetroServer::kick(ENetPeer *peer, const char *message) {
    enet_peer_send(peer, 0, CharArrayPacket(PACKET_KICK, message).pack());
    enet_peer_disconnect(peer, 0);
}

