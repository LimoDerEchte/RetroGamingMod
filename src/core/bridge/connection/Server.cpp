//
// Created by limo on 3/8/25.
//

#include "Server.hpp"

#include <cstring>
#include <iostream>
#include <thread>
#include <headers/com_limo_emumod_bridge_NativeServer.h>

#include "NetworkDefinitions.hpp"
#include "util/util.hpp"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeServer_startServer(JNIEnv *, jclass, const jint port) {
    return reinterpret_cast<jlong>(new RetroServer(port));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeServer_stopServer(JNIEnv *, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroServer*>(ptr);
    server->dispose();
}

JNIEXPORT jstring JNICALL Java_com_limo_emumod_bridge_NativeServer_requestToken(JNIEnv *env, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroServer*>(ptr);
    const auto token = reinterpret_cast<const jchar*>(server->genToken().data());
    return env->NewString(token, 32);
}

RetroServer::RetroServer(const int port) {
    if (enet_initialize() != 0) {
        std::cerr << "[RetroServer] Failed to initialize ENet" << std::endl;
        return;
    }
    ENetAddress address;
    address.host = ENET_HOST_ANY;
    address.port = port;
    server = enet_host_create(&address, SERVER_MAX_CLIENTS, 2, 0, 0);
    if (server == nullptr) {
        std::cerr << "[RetroServer] Failed to create ENet server" << std::endl;
        enet_deinitialize();
        return;
    }
    running = true;
    std::thread([&] {
        mainLoop();
    }).detach();
}

std::array<char, 32> RetroServer::genToken() const {
    std::array<char, 32> stdArr = {0};
    GenerateID(stdArr.data());
    tokens->push_back(stdArr);
    return stdArr;
}

void RetroServer::dispose() {
    running = false;
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
            std::cerr << "[RetroServer] Failed to receive ENet event (" << status << ")" << std::endl;
        } else if (status == 0) {
            continue;
        }
        switch (event.type) {
            case ENET_EVENT_TYPE_NONE: {
                std::cerr << "[RetroServer] Event received an ENET_EVENT_TYPE_NONE" << std::endl;
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

void RetroServer::onConnect(ENetPeer *peer) const {
    const auto client = new RetroServerClient(peer);
    clients->push_back(client);
}

void RetroServer::onDisconnect(ENetPeer *peer) const {
    std::erase_if(*clients, [peer](const RetroServerClient* client) {
        return client->peer == peer;
    });
}

void RetroServer::onMessage(ENetPeer *peer, const ENetPacket *packet) const {
    if (packet->dataLength == 0) {
        std::cerr << "[RetroServer] Received empty packet from " << peer->incomingPeerID << std::endl;
    }
    const auto client = findClientByPeer(peer);
    const auto type = static_cast<PacketType>(packet->data[0]);
    if (type != PACKET_AUTH && !client->isAuthenticated) {
        std::cerr << "[RetroServer] Received non-auth packet before auth from " << peer->incomingPeerID << std::endl;
        return;
    }
    switch (type) {
        case PACKET_AUTH: {
            std::erase_if(*tokens, [packet, client, peer](const std::array<char, 32>& token) {
                if (memcmp(token.data(), &packet->data[1], 32) == 0) {
                    client->isAuthenticated = true;
                    enet_peer_send(peer, 0, enet_packet_create(new char[]{PACKET_AUTH_ACK}, 1, ENET_PACKET_FLAG_RELIABLE));
                    return true;
                }
                return false;
            });
            if (!client->isAuthenticated) {
                kick(peer, "Invalid token");
            }
        }
        case PACKET_UPDATE_CONTROLS: {
            std::cerr << "[RetroServer] Received update controls packet" << std::endl;
            // TODO: Update Controls
        }
        case PACKET_AUTH_ACK:
        case PACKET_KICK:
        case PACKET_UPDATE_DISPLAY:
        case PACKET_UPDATE_AUDIO: {
            std::cerr << "[RetroServer] Received S2C packet on server" << std::endl;
        }
        default: {
            std::cerr << "[RetroServer] Unknown C2S packet type" << std::hex << type << std::endl;
        }
    }
}
