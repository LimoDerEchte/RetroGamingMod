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

public:
    explicit RetroServer(int port);

    void dispose();

    void mainLoop() const;
    void onConnect(ENetPeer* peer) const;
    void onDisconnect(ENetPeer* peer) const;
    static void onMessage(const ENetPeer* peer, const ENetPacket* packet) ;
};
