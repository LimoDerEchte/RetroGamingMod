//
// Created by limo on 3/8/25.
//

#pragma once
#define SERVER_MAX_CLIENTS 100
#include <cstring>
#include <iostream>
#include <string>

enum PacketType {
    // Main Connection
    PACKET_AUTH                 = 0x00,
    PACKET_AUTH_ACK             = 0x01,
    PACKET_KICK                 = 0x02,
    // S2C
    PACKET_UPDATE_DISPLAY       = 0x20,
    PACKET_UPDATE_AUDIO         = 0x21,
    // C2S
    PACKET_UPDATE_CONTROLS      = 0x60,
};

struct KickPacket {
    PacketType type = PACKET_KICK;
    std::vector<char> data;

    explicit KickPacket(const char* msg) {
        data.assign(msg, msg + strlen(msg));
    }

    static std::string* unpack(const ENetPacket* packet) {
        if (packet->dataLength < 5) {
            std::cerr << "[RetroServer] Kick packet too small" << std::endl;
            return nullptr;
        }
        const auto size = reinterpret_cast<int>(&packet->data[1]);
        return new std::string(packet->data[5], size);
    }

    [[nodiscard]] ENetPacket* pack() const {
        int8_t packed[data.size() + 5]{};
        const auto sizeB = reinterpret_cast<int8_t*>(data.size());
        packed[0] = type;
        memcpy(&packed[1], &sizeB, 4);
        memcpy(&packed[5], &data[0], data.size());
        return enet_packet_create(packed, sizeof(packed), ENET_PACKET_FLAG_RELIABLE);
    }
};


