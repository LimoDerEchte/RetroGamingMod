//
// Created by limo on 3/8/25.
//

#include "Client.hpp"

#include <cstring>
#include <iomanip>
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

JNIEXPORT jlong JNICALL Java_com_limo_emumod_client_bridge_NativeClient_registerScreen(JNIEnv *, jclass, const jlong ptr, const jlong jUuid, const jint width, const jint height, const jint sampleRate) {
    const auto client = reinterpret_cast<RetroClient*>(ptr);
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    const auto display = new NativeDisplay(width, height);
    client->registerDisplay(uuid, display, sampleRate);
    return reinterpret_cast<jlong>(display);
}

JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_unregisterScreen(JNIEnv *, jclass, const jlong ptr, const jlong jUuid) {
    const auto client = reinterpret_cast<RetroClient*>(ptr);
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    client->unregisterDisplay(uuid);
}

JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_sendControlUpdate(JNIEnv *, jclass, const jlong ptr, const jlong jUuid, const jint port, const jshort controls) {
    const auto client = reinterpret_cast<RetroClient*>(ptr);
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    client->sendControlsUpdate(uuid, port, controls);
}

JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_updateAudioDistance(JNIEnv *, jclass, const jlong ptr, const jlong jUuid, const jdouble dst) {
    const auto client = reinterpret_cast<RetroClient*>(ptr);
    const auto uuid = reinterpret_cast<jUUID*>(jUuid);
    client->updateAudioDistance(uuid, dst);
}

RetroClient::RetroClient(const char *ip, const int port, const char *token): token(token) {
    std::lock_guard lock(mutex);
    std::lock_guard enet_lock(enet_mutex);
    std::cout << "[RetroClient] Connecting to ENet server on " << ip << ":" << port << std::endl;
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
    std::thread([&] {
        bandwidthMonitorLoop();
    }).detach();
}

void RetroClient::dispose() {
    mutex.lock();
    running = false;
    mutex.unlock();

    while (true) {
        mutex.lock();
        if (runningLoops == 0)
            break;
        mutex.unlock();
        std::this_thread::yield();
    }

    std::lock_guard enet_lock(enet_mutex);
    if (client != nullptr) {
        enet_host_destroy(client);
        client = nullptr;
    }
    enet_deinitialize();
    mutex.unlock();
    std::cout << "[RetroClient] Disconnected from ENet server" << std::endl;
}

void RetroClient::registerDisplay(const jUUID* uuid, NativeDisplay* display, const int sampleRate) {
    std::lock_guard lock(mutex);
    const auto audio = new AudioStreamPlayer(sampleRate, 2);
    audio->start();
    const long uuidCombine = uuid->combine();
    displays.insert_or_assign(uuidCombine, display);
    playbacks.insert_or_assign(uuidCombine, audio);
}

void RetroClient::unregisterDisplay(const jUUID* uuid) {
    std::lock_guard lock(mutex);
    const long uuidCombine = uuid->combine();
    displays.erase(uuidCombine);
    playbacks.erase(uuidCombine);
}

void RetroClient::sendControlsUpdate(const jUUID *link, const int port, const int16_t controls) {
    std::lock_guard lock(mutex);
    int8_t content[3];
    content[0] = static_cast<int8_t>(port);
    memcpy(&content[1], &controls, sizeof(controls));
    std::lock_guard enet_lock(enet_mutex);
    enet_peer_send(peer, 0, Int8ArrayPacket(PACKET_UPDATE_CONTROLS, link, reinterpret_cast<const uint8_t*>(content), sizeof(content)).pack());
    bytesOut += sizeof(content) + 25;
}

void RetroClient::updateAudioDistance(const jUUID *uuid, const double distance) {
    std::lock_guard lock(mutex);
    const auto it = playbacks.find(uuid->combine());
    if (it == playbacks.end()) {
        return;
    }
    it->second->updateDistance(distance);
}

void RetroClient::mainLoop() {
    if (client == nullptr)
        return;
    mutex.lock();
    runningLoops++;
    mutex.unlock();
    while (true) {
        ENetEvent event;
        enet_mutex.lock();
        const auto status = enet_host_service(client, &event, 0);
        enet_mutex.unlock();
        if (status < 0) {
            std::cerr << "[RetroClient] Failed to receive ENet event (" << status << ")" << std::endl;
        } else if (status == 0) {
            std::this_thread::yield();
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
                mutex.lock();
                bytesIn += event.packet->dataLength;
                mutex.unlock();
                onMessage(event.packet);
                break;
            }
        }
        mutex.lock();
        if (!running)
            break;
        mutex.unlock();
    }
    runningLoops--;
    mutex.unlock();
}

void RetroClient::bandwidthMonitorLoop() {
    const auto interval = std::chrono::seconds(5);
    auto lastTime = std::chrono::high_resolution_clock::now();
    uint64_t lastBytesIn = 0;
    uint64_t lastBytesOut = 0;
    mutex.lock();
    runningLoops++;
    mutex.unlock();
    while (true) {
        std::this_thread::sleep_for(interval);
        auto currentTime = std::chrono::high_resolution_clock::now();
        const auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(currentTime - lastTime).count();

        mutex.lock();
        const auto incomingKbps = static_cast<double>(bytesIn - lastBytesIn) * 8 / 1000 / (static_cast<double>(duration) / 1000.0);
        const auto outgoingKbps = static_cast<double>(bytesOut - lastBytesOut) * 8 / 1000 / (static_cast<double>(duration) / 1000.0);
        std::cout << "[RetroClient] Bandwidth: IN: " << std::fixed << std::setprecision(2) << incomingKbps
                  << " kbps, OUT: " << std::fixed << std::setprecision(2) << outgoingKbps << " kbps" << std::endl;

        lastBytesIn = bytesIn;
        lastBytesOut = bytesOut;
        if (!running)
            break;
        mutex.unlock();
        lastTime = currentTime;
    }
    runningLoops--;
    mutex.unlock();
}

void RetroClient::onConnect() {
    std::cout << "[RetroClient] Connection established to " << peer->address.port << std::endl;
    // Auth Packet
    std::lock_guard enet_lock(enet_mutex);
    int8_t pak[33]{};
    pak[0] = PACKET_AUTH;
    memcpy(&pak[1], token, 32);
    enet_peer_send(peer, 0, enet_packet_create(pak, 33, ENET_PACKET_FLAG_RELIABLE));
    mutex.lock();
    bytesOut += 33;
    mutex.unlock();
    std::cout << "[RetroClient] Authorizing with token " << token << std::endl;
}

void RetroClient::onDisconnect() {
    dispose();
}

void RetroClient::onMessage(const ENetPacket *packet) {
    mutex.lock();
    if (!running)
        return;
    mutex.unlock();
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
        case PACKET_KEEP_ALIVE: {
            enet_mutex.lock();
            constexpr int8_t id = PACKET_KEEP_ALIVE;
            enet_peer_send(peer, 0, enet_packet_create(&id, 1, ENET_PACKET_FLAG_RELIABLE));
            enet_mutex.unlock();
            mutex.lock();
            bytesOut += 1;
            mutex.unlock();
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
            const auto parsed = Int8ArrayPacket::unpack(packet);
            if (parsed == nullptr) {
                std::cerr << "[RetroClient] Received invalid display packet" << std::endl;
                return;
            }
            mutex.lock();
            const auto it = displays.find(parsed->ref->combine());
            if (it == displays.end()) {
                std::cerr << "[RetroClient] Received display packet for unknown display " << std::hex << parsed->ref->combine() << std::endl;
                return;
            }
            it->second->receive(parsed->data, parsed->size);
            mutex.unlock();
            break;
        }
        case PACKET_UPDATE_AUDIO: {
            const auto parsed = Int8ArrayPacket::unpack(packet);
            if (parsed == nullptr) {
                std::cerr << "[RetroClient] Received invalid audio packet" << std::endl;
                return;
            }
            mutex.lock();
            const auto it = playbacks.find(parsed->ref->combine());
            if (it == playbacks.end()) {
                std::cerr << "[RetroClient] Received audio packet for unknown display " << std::hex << parsed->ref->combine() << std::endl;
                return;
            }
            it->second->receive(parsed->data, parsed->size);
            mutex.unlock();
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
