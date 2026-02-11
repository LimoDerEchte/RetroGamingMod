//
// Created by limo on 3/8/25.
//

#include "Server.hpp"

#include <cstring>
#include <iostream>
#include <thread>
#include <headers/com_limo_emumod_bridge_NativeServer.h>
#include <algorithm>

#include "NetworkDefinitions.hpp"
#include "platform/GenericConsole.hpp"
#include "util/NativeUtil.hpp"

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeServer_startServer(JNIEnv *, jclass, const jint port, const jint maxUsers) {
    // ReSharper disable once CppDFAMemoryLeak
    return reinterpret_cast<jlong>(new RetroServer(port, maxUsers));
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeServer_stopServer(JNIEnv *, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroServer*>(ptr);
    server->dispose();
    delete server;
}

JNIEXPORT jstring JNICALL Java_com_limo_emumod_bridge_NativeServer_requestToken(JNIEnv *env, jclass, const jlong ptr) {
    const auto server = reinterpret_cast<RetroServer*>(ptr);
    const auto token = server->genToken();
    return env->NewStringUTF(std::string(token, 32).c_str());
}

RetroServer::RetroServer(const int port, const int maxUsers) {
    std::lock_guard lock(mutex);
    std::lock_guard enet_lock(enet_mutex);
    if (enet_initialize() != 0) {
        std::cerr << "[RetroServer] Failed to initialize ENet" << std::endl;
        return;
    }
    ENetAddress address;
    address.host = ENET_HOST_ANY;
    address.port = port;
    server = enet_host_create(&address, maxUsers, 2, 0, 0);
    if (server == nullptr) {
        std::cerr << "[RetroServer] Failed to create ENet server" << std::endl;
        enet_deinitialize();
        return;
    }
    running = true;
    std::thread([&] { mainReceiverLoop(); }).detach();
    std::thread([&] { bandwidthMonitorLoop(); }).detach();
    std::thread([&] { mainKeepAliveLoop(); }).detach();
    std::thread([&] { videoSenderLoop(30); }).detach();
    std::thread([&] { audioSenderLoop(300); }).detach();
    std::cout << "[RetroServer] Started ENet server on port " << port << std::endl;
}

char* RetroServer::genToken() {
    const auto data = new char[32];
    GenerateID(data);
    std::array<char, 32> stdArr = {};
    memcpy(stdArr.data(), data, 32);
    std::lock_guard lock(mutex);
    tokens.push_back(stdArr);
    return data;
}

void RetroServer::dispose() {
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
    if (server != nullptr) {
        enet_host_destroy(server);
        server = nullptr;
    }
    enet_deinitialize();
    mutex.unlock();
    std::cout << "[RetroServer] Stopped ENet server" << std::endl;
}

void RetroServer::mainReceiverLoop() {
    if (server == nullptr)
        return;
    mutex.lock();
    runningLoops++;
    mutex.unlock();
    while (true) {
        ENetEvent event;
        enet_mutex.lock();
        const auto status = enet_host_service(server, &event, 0);
        enet_mutex.unlock();
        if (status < 0) {
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
                mutex.lock();
                bytesIn += event.packet->dataLength;
                mutex.unlock();
                onMessage(event.peer, event.packet);
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

void RetroServer::bandwidthMonitorLoop() {
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
        std::cout << "[RetroServer] Bandwidth: IN: " << std::fixed << std::setprecision(2) << incomingKbps
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

void RetroServer::mainKeepAliveLoop() {
    const auto delay = std::chrono::seconds(5);
    auto next = std::chrono::high_resolution_clock::now();
    mutex.lock();
    runningLoops++;
    mutex.unlock();
    while (true) {
        mutex.lock();
        for (const auto& client : clients) {
            if (client == nullptr || client->peer == nullptr || client->peer->state != ENET_PEER_STATE_CONNECTED)
                continue;
            constexpr int8_t id = PACKET_KEEP_ALIVE;
            enet_mutex.lock();
            enet_peer_send(client->peer, 0, enet_packet_create(&id, 1, ENET_PACKET_FLAG_RELIABLE));
            enet_mutex.unlock();
            bytesOut++;
        }
        if (!running)
            break;
        mutex.unlock();
        next += delay;
        std::this_thread::sleep_until(next);
    }
    runningLoops--;
    mutex.unlock();
}

void RetroServer::videoSenderLoop(const int fps) {
    const auto delay = std::chrono::nanoseconds(1000000000 / fps);
    auto next = std::chrono::high_resolution_clock::now();
    mutex.lock();
    runningLoops++;
    mutex.unlock();
    while (true) {
        GenericConsoleRegistry::withConsoles(true, [this](const auto console) {
            if (!console->retroCoreHandle || !console->retroCoreHandle->displayChanged)
                return;
            const auto frame = console->createFrame();
            if (frame.empty())
                return;
            const auto packet = Int8ArrayPacket(PACKET_UPDATE_DISPLAY, console->consoleId, frame).pack();
            mutex.lock();
            for (const auto& client : clients) {
                if (client == nullptr || client->peer == nullptr || client->peer->state != ENET_PEER_STATE_CONNECTED)
                    continue;
                enet_mutex.lock();
                enet_peer_send(client->peer, 0, packet);
                enet_mutex.unlock();
                bytesOut += packet->dataLength;
            }
            mutex.unlock();
        });
        mutex.lock();
        if (!running)
            break;
        mutex.unlock();
        next += delay;
        std::this_thread::sleep_until(next);
    }
    runningLoops--;
    mutex.unlock();
}

void RetroServer::audioSenderLoop(const int cps) {
    const auto delay = std::chrono::nanoseconds(1000000000 / cps);
    auto next = std::chrono::high_resolution_clock::now();
    mutex.lock();
    runningLoops++;
    mutex.unlock();
    while (true) {
        GenericConsoleRegistry::withConsoles(false, [this](const auto console) {
            if (!console->retroCoreHandle->audioChanged)
                return;
            const auto frame = console->createClip();
            console->retroCoreHandle->audioChanged = false;
            if (frame.empty())
                return;
            const auto packet = Int8ArrayPacket(PACKET_UPDATE_AUDIO, console->consoleId, frame).pack();
            mutex.lock();
            for (const auto& client : clients) {
                if (client == nullptr || client->peer == nullptr || client->peer->state != ENET_PEER_STATE_CONNECTED)
                    continue;
                enet_mutex.lock();
                enet_peer_send(client->peer, 0, packet);
                enet_mutex.unlock();
                bytesOut += packet->dataLength;
            }
            mutex.unlock();
        });
        mutex.lock();
        if (!running)
            break;
        mutex.unlock();
        next += delay;
        std::this_thread::sleep_until(next);
    }
    runningLoops--;
    mutex.unlock();
}

void RetroServer::onConnect(ENetPeer *peer) {
    std::lock_guard lock(mutex);
    clients.push_back(std::make_shared<RetroServerClient>(peer));
}

void RetroServer::onDisconnect(ENetPeer *peer) {
    std::lock_guard lock(mutex);
    std::erase_if(clients, [peer](const auto& client) {
        return client != nullptr && client->peer == peer;
    });
}

void RetroServer::onMessage(ENetPeer *peer, const ENetPacket *packet) {
    mutex.lock();
    if (!running)
        return;
    mutex.unlock();
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
            std::erase_if(tokens, [&, packet, client, peer](const std::array<char, 32>& token) {
                if (memcmp(token.data(), &packet->data[1], 32) == 0) {
                    client->isAuthenticated = true;
                    enet_mutex.lock();
                    enet_peer_send(peer, 0, enet_packet_create(new char[]{ PACKET_AUTH_ACK }, 1, ENET_PACKET_FLAG_RELIABLE));
                    enet_mutex.unlock();
                    bytesOut += 1;
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
            break;
        }
        case PACKET_UPDATE_CONTROLS: {
            const auto parsed = Int8ArrayPacket::unpack(packet);
            auto* p = parsed.get();
            const auto pCombine = p->ref->combine();
            GenericConsoleRegistry::withConsoles(false, [p, pCombine](const GenericConsole* entry) {
                if (entry->consoleId->combine() != pCombine)
                    return;
                const int port = p->data[0];
                union {
                    uint8_t bytes[2];
                    int16_t value = 0;
                } converter;
                converter.bytes[0] = p->data[1];
                converter.bytes[1] = p->data[2];
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

void RetroServer::kick(ENetPeer *peer, const char *message) {
    std::lock_guard enet_lock(enet_mutex);
    const auto packet = CharArrayPacket(PACKET_KICK, message).pack();
    enet_peer_send(peer, 0, packet);
    enet_peer_disconnect(peer, 0);
    bytesOut += packet->dataLength;
}

std::shared_ptr<RetroServerClient> RetroServer::findClientByPeer(const ENetPeer* peer) const {
    for (const auto& element : clients) {
        if (element == nullptr || element->peer != peer) {
            continue;
        }
        return element;
    }
    return nullptr;
}
