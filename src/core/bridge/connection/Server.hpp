//
// Created by limo on 3/8/25.
//

#pragma once
#include <mutex>
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
    std::mutex mutex;
    std::vector<RetroServerClient*>* clients = new std::vector<RetroServerClient*>(SERVER_MAX_CLIENTS);
    std::vector<std::array<char, 32>>* tokens = new std::vector<std::array<char, 32>>();

public:
    explicit RetroServer(int port);

    [[nodiscard]] char* genToken();
    void dispose();

    void mainReceiverLoop();
    void mainKeepAliveLoop();
    void mainVideoSenderLoop(int fps);
    void onConnect(ENetPeer* peer) const;
    void onDisconnect(ENetPeer* peer) const;
    void onMessage(ENetPeer* peer, const ENetPacket* packet);

    static void kick(ENetPeer *peer, const char *message);
    RetroServerClient* findClientByPeer(const ENetPeer* peer) const;
};

inline RetroServerClient* RetroServer::findClientByPeer(const ENetPeer* peer) const {
    for (const auto element : *clients) {
        if (element == nullptr || element->peer != peer) {
            continue;
        }
        return element;
    }
    return nullptr;
}

inline void RetroServer::kick(ENetPeer *peer, const char *message) {
    enet_peer_send(peer, 0, CharArrayPacket(PACKET_KICK, message).pack());
    enet_peer_disconnect(peer, 0);
}

