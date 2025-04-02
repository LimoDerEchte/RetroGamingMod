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
    std::mutex enet_mutex;
    ENetHost* server;
    bool running = false;
    int runningLoops = 0;
    std::mutex mutex;
    std::vector<RetroServerClient*>* clients = new std::vector<RetroServerClient*>(SERVER_MAX_CLIENTS);
    std::vector<std::array<char, 32>>* tokens = new std::vector<std::array<char, 32>>();

    uint64_t bytesIn = 0;
    uint64_t bytesOut = 0;

public:
    explicit RetroServer(int port);

    [[nodiscard]] char* genToken();
    void dispose();

    void mainReceiverLoop();
    void bandwidthMonitorLoop();
    void mainKeepAliveLoop();
    void videoSenderLoop(int fps);
    void audioSenderLoop(int cps);
    void onConnect(ENetPeer* peer);
    void onDisconnect(ENetPeer* peer);
    void onMessage(ENetPeer* peer, const ENetPacket* packet);

    void kick(ENetPeer *peer, const char *message);
    RetroServerClient* findClientByPeer(const ENetPeer* peer) const;
};
