//
// Created by limo on 3/8/25.
//

#include "Server.hpp"

#include <iostream>
#include <thread>
#include <headers/com_limo_emumod_bridge_NativeServer.h>

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeServer_startServer(JNIEnv *, jclass, const jint port) {
    return reinterpret_cast<jlong>(new RetroServer(port));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeServer_stopServer(JNIEnv *, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroServer*>(ptr);
    server->dispose();
}

RetroServer::RetroServer(const int port) {
    if (enet_initialize() != 0) {
        std::cerr << "Failed to initialize ENet." << std::endl;
        return;
    }
    ENetAddress address;
    address.host = ENET_HOST_ANY;
    address.port = port;
    server = enet_host_create(&address, SERVER_MAX_CLIENTS, 2, 0, 0);
    if (server == nullptr) {
        std::cerr << "Failed to create ENet server." << std::endl;
        enet_deinitialize();
        return;
    }
    running = true;
    std::thread([&] {
        mainLoop();
    }).detach();
}

void RetroServer::dispose() {
    if (server != nullptr) {
        enet_host_destroy(server);
        server = nullptr;
    }
    enet_deinitialize();
}

void RetroServer::mainLoop() const {
    if (server == nullptr)
        return;
    while (running) {
        ENetEvent event;
        if (const auto status = enet_host_service(server, &event, 0); status < 0) {
            std::cerr << "Failed to receive ENet event. (" << status << ")" << std::endl;
        } else if (status == 0) {
            continue;
        }
        switch (event.type) {
            case ENET_EVENT_TYPE_NONE: {
                std::cerr << "Event received an ENET_EVENT_TYPE_NONE." << std::endl;
            }
            case ENET_EVENT_TYPE_CONNECT: {
                onConnect(event.peer);
            }
            case ENET_EVENT_TYPE_DISCONNECT: {
                onDisconnect(event.peer);
            }
            case ENET_EVENT_TYPE_RECEIVE: {
                onMessage(event.peer, event.packet);
            }
        }
    }
}

void RetroServer::onConnect(ENetPeer *peer) const { // NOLINT(*-convert-member-functions-to-static)
    const auto client = new RetroServerClient(peer);
    clients->push_back(client);
}

void RetroServer::onDisconnect(ENetPeer *peer) const {
    std::erase_if(*clients, [peer](const RetroServerClient* client) {
        return client->peer == peer;
    });
}

void RetroServer::onMessage(const ENetPeer *peer, const ENetPacket *packet) {
    if (packet->dataLength == 0) {
        std::cerr << "Received empty packet from " << peer->incomingPeerID << std::endl;
    }
    const auto type = static_cast<PacketType>(packet->data[0]);
    // TODO: Actually implement packets
    switch (type) {
        case PACKET_AUTH:
        case PACKET_AUTH_ACK:
        case PACKET_KICK:
        case PACKET_UPDATE_DISPLAY:
        case PACKET_UPDATE_AUDIO:
        case PACKET_UPDATE_CONTROLS:
        default: {
            std::cerr << "Unknown C2S packet type " << std::hex << type << std::endl;
        }
    }
}
