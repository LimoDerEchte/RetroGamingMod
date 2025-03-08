//
// Created by limo on 3/8/25.
//

#pragma once
#include <cstring>
#include <iostream>

#define SERVER_MAX_CLIENTS 100

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

struct CharArrayPacket {
    PacketType type;
    std::vector<char> data;

    explicit CharArrayPacket(const PacketType type, const char* msg) : type(type) {
        data.assign(msg, msg + strlen(msg));
    }

    explicit CharArrayPacket(const PacketType type, const unsigned char* ptr, const size_t size) : type(type) {
        data.assign(ptr, ptr + size);
    }

    static CharArrayPacket* unpack(const ENetPacket* packet) {
        const auto type = static_cast<PacketType>(packet->data[0]);
        if (packet->dataLength < 9) {
            std::cerr << "[RetroPacket] Char packet too small (" << type << ")" << std::endl;
            return nullptr;
        }
        size_t size = 0;
        memcpy(&size, &packet->data[1], sizeof(size_t));
        return new CharArrayPacket(type, &packet->data[9], size);
    }

    [[nodiscard]] ENetPacket* pack() const {
        void* packed = malloc(data.size() + 9);
        const auto sizeB = reinterpret_cast<int8_t*>(data.size());
        memcpy(&packed[0], &type, 1);
        memcpy(&packed[1], &sizeB, sizeof(size_t));
        memcpy(&packed[9], &data[0], data.size());
        return enet_packet_create(packed, sizeof(packed), ENET_PACKET_FLAG_RELIABLE);
    }
};

struct Int8ArrayPacket {
    PacketType type;
    std::vector<int8_t> data;

    explicit Int8ArrayPacket(const PacketType type, const unsigned char* ptr, const size_t size) : type(type) {
        data.assign(ptr, ptr + size);
    }

    static Int8ArrayPacket* unpack(const ENetPacket* packet) {
        const auto type = static_cast<PacketType>(packet->data[0]);
        if (packet->dataLength < 9) {
            std::cerr << "[RetroPacket] Int8arr packet too small (" << type << ")" << std::endl;
            return nullptr;
        }
        size_t size = 0;
        memcpy(&size, &packet->data[1], sizeof(size_t));
        return new Int8ArrayPacket(type, &packet->data[9], size);
    }

    [[nodiscard]] ENetPacket* pack() const {
        void* packed = malloc(data.size() + 9);
        const auto sizeB = reinterpret_cast<int8_t*>(data.size());
        memcpy(&packed[0], &type, 1);
        memcpy(&packed[1], &sizeB, sizeof(size_t));
        memcpy(&packed[9], &data[0], data.size());
        return enet_packet_create(packed, sizeof(packed), 0);
    }
};

struct Int16ArrayPacket {
    PacketType type;
    std::vector<int16_t> data;

    explicit Int16ArrayPacket(const PacketType type, const unsigned char* ptr, const size_t size) : type(type) {
        data.assign(ptr, ptr + size);
    }

    static Int16ArrayPacket* unpack(const ENetPacket* packet) {
        const auto type = static_cast<PacketType>(packet->data[0]);
        if (packet->dataLength < 9) {
            std::cerr << "[RetroPacket] Int16arr packet too small (" << type << ")" << std::endl;
            return nullptr;
        }
        size_t size = 0;
        memcpy(&size, &packet->data[1], sizeof(size_t));
        return new Int16ArrayPacket(type, &packet->data[9], size);
    }

    [[nodiscard]] ENetPacket* pack() const {
        void* packed = malloc(data.size() * 2 + 9);
        const auto sizeB = reinterpret_cast<int16_t*>(data.size());
        memcpy(&packed[0], &type, 1);
        memcpy(&packed[1], &sizeB, sizeof(size_t));
        memcpy(&packed[9], &data[0], data.size() * 2);
        return enet_packet_create(packed, sizeof(packed), 0);
    }
};
