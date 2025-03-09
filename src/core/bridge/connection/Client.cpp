//
// Created by limo on 3/8/25.
//

#include "Client.hpp"

#include <cstring>
#include <iostream>
#include <thread>
#include <headers/com_limo_emumod_client_bridge_NativeClient.h>

#include "NetworkDefinitions.hpp"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_client_bridge_NativeClient_connect(JNIEnv *env, jclass, const jstring ip, const jint port, const jstring token) {
    return reinterpret_cast<jlong>(new RetroClient(
        env->GetStringUTFChars(ip, nullptr), port, env->GetStringUTFChars(token, nullptr)
    ));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_disconnect(JNIEnv *, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroClient*>(ptr);
    server->dispose();
}

JNIEXPORT jboolean JNICALL Java_com_limo_emumod_client_bridge_NativeClient_isAuthenticated(JNIEnv *, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroClient*>(ptr);
    return server->isAuthenticated();
}

RetroClient::RetroClient(const char *ip, const int port, const char *token): displays() {
    if (enet_initialize() != 0) {
        std::cerr << "[RetroClient] Failed to initialize ENet" << std::endl;
        return;
    }
    ENetAddress address;
    enet_address_set_host(&address, ip);
    address.port = port;
    client = enet_host_create(nullptr, 1, 2, 0, 0);
    if (client == nullptr) {
        std::cerr << "[RetroClient] Failed to create ENet client" << std::endl;
        enet_deinitialize();
        return;
    }
    peer = enet_host_connect(client, &address, 2, 0);
    if (peer == nullptr) {
        std::cerr << "[RetroClient] Failed to connect to ENet server" << std::endl;
        enet_deinitialize();
        return;
    }
    running = true;
    std::thread([&] {
        mainLoop();
    }).detach();
    // Auth Packet
    int8_t pak[33]{};
    memset(pak, PACKET_AUTH, 1);
    memcpy(&pak[1], token, 32);
    enet_peer_send(peer, 0, enet_packet_create(pak, 33, ENET_PACKET_FLAG_RELIABLE));
}

void RetroClient::dispose() {
    running = false;
    if (client != nullptr) {
        enet_host_destroy(client);
        client = nullptr;
    }
    enet_deinitialize();
}

void RetroClient::mainLoop() {
    if (client == nullptr)
        return;
    while (running) {
        ENetEvent event;
        if (const auto status = enet_host_service(client, &event, 0); status < 0) {
            std::cerr << "[RetroClient] Failed to receive ENet event (" << status << ")" << std::endl;
        } else if (status == 0) {
            continue;
        }
        switch (event.type) {
            case ENET_EVENT_TYPE_NONE: {
                std::cerr << "[RetroClient] Event received an ENET_EVENT_TYPE_NONE" << std::endl;
            }
            case ENET_EVENT_TYPE_CONNECT: {
                onConnect();
            }
            case ENET_EVENT_TYPE_DISCONNECT: {
                onDisconnect();
            }
            case ENET_EVENT_TYPE_RECEIVE: {
                onMessage(event.packet);
            }
        }
    }
}

void RetroClient::onConnect() const {
    std::cerr << "[RetroClient] Connection established on " << peer->address.port << std::endl;
}

void RetroClient::onDisconnect() {
    std::cerr << "[RetroClient] Disconnected on " << peer->address.port << std::endl;
    dispose();
}

void RetroClient::onMessage(const ENetPacket *packet) {
    if (packet->dataLength == 0) {
        std::cerr << "[RetroClient] Received empty packet from server" << std::endl;
    }
    switch (const auto type = static_cast<PacketType>(packet->data[0])) {
        case PACKET_AUTH_ACK: {
            authenticated = true;
        }
        case PACKET_KICK: {
            const auto kick = CharArrayPacket::unpack(packet);
            if (kick == nullptr) {
                return;
            }
            const char* str = &kick->data[0];
            std::cerr << "[RetroClient] Received kick packet: " << std::string(str, strlen(str)) << std::endl;
        }
        case PACKET_UPDATE_DISPLAY: {
            std::cerr << "[RetroClient] Received update display packet" << std::endl;
            // TODO: Update Display
        }
        case PACKET_UPDATE_AUDIO: {
            std::cerr << "[RetroClient] Received update audio packet" << std::endl;
            // TODO: Update Audio
        }
        case PACKET_AUTH:
        case PACKET_UPDATE_CONTROLS: {
            std::cerr << "[RetroClient] Received C2S packet on client" << std::endl;
        }
        default: {
            std::cerr << "[RetroClient] Unknown C2S packet type" << std::hex << type << std::endl;
        }
    }
}
