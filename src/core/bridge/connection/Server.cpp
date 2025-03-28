//
// Created by limo on 3/8/25.
//

#include "Server.hpp"

#include <cstring>
#include <iostream>
#include <thread>
#include <headers/com_limo_emumod_bridge_NativeServer.h>

#include "NetworkDefinitions.hpp"
#include "platform/GenericConsole.hpp"
#include "util/NativeUtil.hpp"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeServer_startServer(JNIEnv *, jclass, const jint port) {
    return reinterpret_cast<jlong>(new RetroServer(port));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeServer_stopServer(JNIEnv *, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroServer*>(ptr);
    server->dispose();
}

JNIEXPORT jstring JNICALL Java_com_limo_emumod_bridge_NativeServer_requestToken(JNIEnv *env, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroServer*>(ptr);
    const auto token = server->genToken();
    return env->NewStringUTF(std::string(token, 32).c_str());
}

RetroServer::RetroServer(const int port) {
    std::lock_guard lock(mutex);
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
    std::thread([&] { mainReceiverLoop(); }).detach();
    std::thread([&] { mainKeepAliveLoop(); }).detach();
    std::thread([&] { mainSenderLoop(30); }).detach();
    std::cout << "[RetroServer] Started ENet server on port " << port << std::endl;
}

char* RetroServer::genToken() {
    std::lock_guard lock(mutex);
    const auto data = new char[32];
    GenerateID(data);
    std::array<char, 32> stdArr = {};
    memcpy(stdArr.data(), data, 32);
    tokens->push_back(stdArr);
    return data;
}

void RetroServer::dispose() {
    std::lock_guard lock(mutex);
    running = false;
    if (server != nullptr) {
        enet_host_destroy(server);
        server = nullptr;
    }
    enet_deinitialize();
    std::cout << "[RetroServer] Stopped ENet server" << std::endl;
}

void RetroServer::mainReceiverLoop() {
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
                break;
            }
            case ENET_EVENT_TYPE_CONNECT: {
                onConnect(event.peer);
                break;
            }
            case ENET_EVENT_TYPE_DISCONNECT: {
                onDisconnect(event.peer);
                break;
            }
            case ENET_EVENT_TYPE_RECEIVE: {
                onMessage(event.peer, event.packet);
                break;
            }
        }
    }
}

void RetroServer::mainKeepAliveLoop() {
    const auto delay = std::chrono::seconds(5);
    auto next = std::chrono::high_resolution_clock::now();
    while (running) {
        mutex.lock();
        for (const RetroServerClient* client : *clients) {
            if (client == nullptr || client->peer == nullptr || client->peer->state != ENET_PEER_STATE_CONNECTED)
                continue;
            constexpr int8_t id = PACKET_KEEP_ALIVE;
            enet_peer_send(client->peer, 0, enet_packet_create(&id, 1, ENET_PACKET_FLAG_RELIABLE));
        }
        mutex.unlock();
        next += delay;
        std::this_thread::sleep_until(next);
    }
}

void RetroServer::mainSenderLoop(const int fps) {
    const auto delay = std::chrono::nanoseconds(1000000000 / fps);
    auto next = std::chrono::high_resolution_clock::now();
    while (running) {
        // Pack and send Video Data
        GenericConsoleRegistry::withConsoles([this](const auto console) {
            if (!console->retroCoreHandle->displayChanged)
                return;
            const auto frame = console->createFrame();
            if (frame.empty())
                return;
            const auto packet = Int8ArrayPacket(
                PACKET_UPDATE_DISPLAY, console->uuid,
                frame.data(), frame.size()
            ).pack();
            mutex.lock();
            for (const RetroServerClient* client : *clients) {
                if (client == nullptr || client->peer == nullptr || client->peer->state != ENET_PEER_STATE_CONNECTED)
                    continue;
                enet_peer_send(client->peer, 0, packet);
            }
            mutex.unlock();
            delete[] packet;
        });
        // Pack and send Audio Data
        GenericConsoleRegistry::withConsoles([this](const auto console) {
            if (!console->retroCoreHandle->audioChanged)
                return;
            const auto frame = console->createClip();
            console->retroCoreHandle->audioChanged = false;
            if (frame.empty())
                return;
            const auto packet = Int8ArrayPacket(
                PACKET_UPDATE_AUDIO, console->uuid,
                frame.data(), frame.size()
            ).pack();
            mutex.lock();
            for (const RetroServerClient* client : *clients) {
                if (client == nullptr || client->peer == nullptr || client->peer->state != ENET_PEER_STATE_CONNECTED)
                    continue;
                enet_peer_send(client->peer, 0, packet);
            }
            mutex.unlock();
            delete[] packet;
        });
        next += delay;
        std::this_thread::sleep_until(next);
    }
}

void RetroServer::onConnect(ENetPeer *peer) const {
    const auto client = new RetroServerClient(peer);
    clients->push_back(client);
}

void RetroServer::onDisconnect(ENetPeer *peer) const {
    std::erase_if(*clients, [peer](const RetroServerClient* client) {
        return client != nullptr && client->peer == peer;
    });
}

void RetroServer::onMessage(ENetPeer *peer, const ENetPacket *packet) {
    if (packet == nullptr) {
        std::cerr << "[RetroServer] Received packet is nullptr from " << peer->incomingPeerID << std::endl;
        return;
    }
    if (packet->dataLength == 0) {
        std::cerr << "[RetroServer] Received empty packet from " << peer->incomingPeerID << std::endl;
        return;
    }
    const auto client = findClientByPeer(peer);
    const auto type = static_cast<PacketType>(packet->data[0]);
    if (type != PACKET_AUTH && !client->isAuthenticated) {
        std::cerr << "[RetroServer] Received non-auth packet before auth from " << peer->incomingPeerID << std::endl;
        return;
    }
    switch (type) {
        case PACKET_AUTH: {
            if (client->isAuthenticated) {
                std::cerr << "[RetroServer] Received auth packet after auth from " << peer->incomingPeerID << std::endl;
                return;
            }
            mutex.lock();
            std::erase_if(*tokens, [packet, client, peer](const std::array<char, 32>& token) {
                if (memcmp(token.data(), &packet->data[1], 32) == 0) {
                    client->isAuthenticated = true;
                    enet_peer_send(peer, 0, enet_packet_create(new char[]{ PACKET_AUTH_ACK }, 1, ENET_PACKET_FLAG_RELIABLE));
                    return true;
                }
                return false;
            });
            if (!client->isAuthenticated) {
                kick(peer, "Invalid token");
                std::cerr << "[RetroServer] Kicking connection with invalid token (" << client->peer->incomingPeerID << ")" << std::endl;
            } else {
                std::cout << "[RetroServer] Successfully authorized connection (" << client->peer->incomingPeerID << ")" << std::endl;
            }
            mutex.unlock();
            break;
        }
        case PACKET_KEEP_ALIVE: {
            std::cout << "[RetroServer] Received Keep Alive from " << client->peer->incomingPeerID << std::endl;
            break;
        }
        case PACKET_UPDATE_CONTROLS: {
            const auto parsed = Int8ArrayPacket::unpack(packet);
            GenericConsoleRegistry::withConsole(parsed->ref, [parsed](const GenericConsole* entry) {
                const int port = parsed->data[0];
                union {
                    uint8_t bytes[2];
                    int16_t value = 0;
                } converter;
                converter.bytes[0] = parsed->data[1];
                converter.bytes[1] = parsed->data[2];
                entry->input(port, converter.value);
            });
            break;
        }
        case PACKET_AUTH_ACK:
        case PACKET_KICK:
        case PACKET_UPDATE_DISPLAY:
        case PACKET_UPDATE_AUDIO: {
            std::cerr << "[RetroServer] Received S2C packet on server" << std::endl;
            break;
        }
        default: {
            std::cerr << "[RetroServer] Unknown C2S packet type" << std::hex << type << std::endl;
            break;
        }
    }
}
