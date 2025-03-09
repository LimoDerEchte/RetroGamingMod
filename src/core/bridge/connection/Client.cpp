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
    const auto client = reinterpret_cast<RetroClient*>(ptr);
    client->dispose();
}

JNIEXPORT jboolean JNICALL Java_com_limo_emumod_client_bridge_NativeClient_isAuthenticated(JNIEnv *, jclass, const jlong ptr) {
    const auto client = reinterpret_cast<RetroClient*>(ptr);
    return client->isAuthenticated();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_registerScreen(JNIEnv *, jclass, const jlong ptr, const jlong jUuid, const jint width, const jint height) {
    const auto client = reinterpret_cast<RetroClient*>(ptr);
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    client->registerDisplay(uuid, width, height);
}

JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_unregisterScreen(JNIEnv *, jclass, const jlong ptr, const jlong jUuid) {
    const auto client = reinterpret_cast<RetroClient*>(ptr);
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    client->unregisterDisplay(uuid);
}

RetroClient::RetroClient(const char *ip, const int port, const char *token): token(token) {
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
}

void RetroClient::dispose() {
    running = false;
    if (client != nullptr) {
        enet_host_destroy(client);
        client = nullptr;
    }
    enet_deinitialize();
    std::cout << "[RetroClient] Disconnected from ENet server" << std::endl;
}

void RetroClient::registerDisplay(const jUUID* uuid, const int width, const int height) {
    displays.insert_or_assign(uuid->combine(), new NativeDisplay(width, height));
}

void RetroClient::unregisterDisplay(const jUUID* uuid) {
    displays.erase(uuid->combine());
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
                break;
            }
            case ENET_EVENT_TYPE_CONNECT: {
                onConnect();
                break;
            }
            case ENET_EVENT_TYPE_DISCONNECT: {
                onDisconnect();
                break;
            }
            case ENET_EVENT_TYPE_RECEIVE: {
                onMessage(event.packet);
                break;
            }
        }
    }
}

void RetroClient::onConnect() const {
    std::cout << "[RetroClient] Connection established to " << peer->address.port << std::endl;
    // Auth Packet
    int8_t pak[33]{};
    pak[0] = PACKET_AUTH;
    memcpy(&pak[1], token, 32);
    enet_peer_send(peer, 0, enet_packet_create(pak, 33, ENET_PACKET_FLAG_RELIABLE));
    std::cout << "[RetroClient] Authorizing with token " << token << std::endl;
}

void RetroClient::onDisconnect() {
    dispose();
}

void RetroClient::onMessage(const ENetPacket *packet) {
    if (packet == nullptr) {
        std::cerr << "[RetroClient] Received packet is nullptr" << std::endl;
        return;
    }
    if (packet->dataLength == 0) {
        std::cerr << "[RetroClient] Received empty packet from server" << std::endl;
        return;
    }
    switch (const auto type = static_cast<PacketType>(packet->data[0])) {
        case PACKET_AUTH_ACK: {
            authenticated = true;
            std::cout << "[RetroClient] Connection token accepted by server" << std::endl;
            break;
        }
        case PACKET_KICK: {
            const auto kick = CharArrayPacket::unpack(packet);
            if (kick == nullptr) {
                return;
            }
            const char* str = &kick->data[0];
            std::cerr << "[RetroClient] Received kick packet: " << std::string(str, strlen(str)) << std::endl;
            break;
        }
        case PACKET_UPDATE_DISPLAY: {
            std::cout << "[RetroClient] Received update display packet" << std::endl;
            const auto parsed = Int16ArrayPacket::unpack(packet);
            if (parsed == nullptr) {
                return;
            }
            const auto it = displays.find(parsed->ref.combine());
            if (it == displays.end()) {
                return;
            }
            memcpy(it->second->buf, &parsed->data[0], parsed->data.size() * 2);
            [](bool& ref) {
                ref = true;
            }(*it->second->changed);
            break;
        }
        case PACKET_UPDATE_AUDIO: {
            std::cout << "[RetroClient] Received update audio packet" << std::endl;
            // TODO: Update Audio
            break;
        }
        case PACKET_AUTH:
        case PACKET_UPDATE_CONTROLS: {
            std::cerr << "[RetroClient] Received C2S packet on client" << std::endl;
            break;
        }
        default: {
            std::cerr << "[RetroClient] Unknown C2S packet type" << std::hex << type << std::endl;
            break;
        }
    }
}
