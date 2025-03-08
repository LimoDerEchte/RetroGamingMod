//
// Created by limo on 3/8/25.
//

#include "Client.hpp"

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

RetroClient::RetroClient(const char *ip, const int port, const char *token) {
    if (enet_initialize() != 0) {
        std::cerr << "[RetroServer] Failed to initialize ENet" << std::endl;
        return;
    }
    ENetAddress address;
    enet_address_set_host(&address, ip);
    address.port = port;
    client = enet_host_create(nullptr, 1, 2, 0, 0);
    if (client == nullptr) {
        std::cerr << "[RetroServer] Failed to create ENet client" << std::endl;
        enet_deinitialize();
        return;
    }
    peer = enet_host_connect(client, &address, 2, 0);
    if (peer == nullptr) {
        std::cerr << "[RetroServer] Failed to connect to ENet server" << std::endl;
        enet_deinitialize();
        return;
    }
    running = true;
    std::thread([&] {
        mainLoop();
    }).detach();
    // Auth Packet
    void* pak = malloc(33);
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

void RetroClient::mainLoop() const {
    if (client == nullptr)
        return;
    while (running) {
        ENetEvent event;
        if (const auto status = enet_host_service(client, &event, 0); status < 0) {
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
