//
// Created by limo on 3/8/25.
//

#pragma once
#include <vector>
#include <enet/enet.h>

#include "util/NativeUtil.hpp"

#define SERVER_MAX_CLIENTS 100

enum PacketType {
    // Main Connection
    PACKET_AUTH                 = 0x00,
    PACKET_AUTH_ACK             = 0x01,
    PACKET_KICK                 = 0x02,
    PACKET_KEEP_ALIVE           = 0x03,
    // S2C
    PACKET_UPDATE_DISPLAY       = 0x20,
    PACKET_UPDATE_AUDIO         = 0x21,
    // C2S
    PACKET_UPDATE_CONTROLS      = 0x60,
};

struct CharArrayPacket {
    PacketType type;
    std::vector<char> data;

    explicit CharArrayPacket(PacketType type, const char* msg);
    explicit CharArrayPacket(PacketType type, const unsigned char* ptr, size_t size);

    static CharArrayPacket* unpack(const ENetPacket* packet);
    [[nodiscard]] ENetPacket* pack() const;
};

struct Int16ArrayPacket {
    PacketType type;
    const jUUID* ref;
    void* data;
    const size_t size;

    explicit Int16ArrayPacket(PacketType type, const jUUID* ref, const unsigned char* ptr, size_t size);

    static Int16ArrayPacket* unpack(const ENetPacket* packet);
    [[nodiscard]] ENetPacket* pack() const;
};
