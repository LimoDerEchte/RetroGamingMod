//
// Created by limo on 3/8/25.
//

#include "NetworkDefinitions.hpp"

#include <cstring>
#include <iostream>

CharArrayPacket::CharArrayPacket(const PacketType type, const char* msg) : type(type) {
    data.assign(msg, msg + strlen(msg));
}

CharArrayPacket::CharArrayPacket(const PacketType type, const unsigned char *ptr, const size_t size) : type(type) {
    data.assign(ptr, ptr + size);
}

CharArrayPacket* CharArrayPacket::unpack(const ENetPacket *packet) {
    const auto type = static_cast<PacketType>(packet->data[0]);
    if (packet->dataLength < 9) {
        std::cerr << "[RetroPacket] Char packet too small (" << type << ")" << std::endl;
        return nullptr;
    }
    size_t size = 0;
    memcpy(&size, &packet->data[1], sizeof(size_t));
    return new CharArrayPacket(type, &packet->data[9], size);
}

ENetPacket* CharArrayPacket::pack() const {
    const auto packetSize = data.size() + 9;
    const auto packed = new int8_t[packetSize]{};
    const auto sizeB = reinterpret_cast<int8_t*>(data.size());
    memcpy(&packed[0], &type, 1);
    memcpy(&packed[1], &sizeB, sizeof(size_t));
    memcpy(&packed[9], &data[0], data.size());
    return enet_packet_create(packed, packetSize, ENET_PACKET_FLAG_RELIABLE);
}

Int8ArrayPacket::Int8ArrayPacket(const PacketType type, const jUUID* ref, const uint8_t* ptr, const size_t size)
    : type(type), ref(ref), data(ptr), size(size) { }

Int8ArrayPacket* Int8ArrayPacket::unpack(const ENetPacket *packet) {
    const auto type = static_cast<PacketType>(packet->data[0]);
    if (packet->dataLength < 25) {
        std::cerr << "[RetroPacket] Int16arr packet too small (" << type << ")" << std::endl;
        return nullptr;
    }
    size_t size = 0;
    const auto uuid = new jUUID();
    memcpy(uuid, &packet->data[1], sizeof(jUUID));
    memcpy(&size, &packet->data[17], sizeof(size_t));
    return new Int8ArrayPacket(type, uuid, &packet->data[25], size);
}

ENetPacket* Int8ArrayPacket::pack() const{
    const auto packetSize = size + 25;
    const auto packed = new int8_t[packetSize]{};
    const auto sizeB = reinterpret_cast<int16_t*>(size);
    memcpy(&packed[0], &type, 1);
    memcpy(&packed[1], ref, sizeof(jUUID));
    memcpy(&packed[17], &sizeB, sizeof(size_t));
    memcpy(&packed[25], data, size);
    const auto packet = enet_packet_create(packed, packetSize, 0);
    return packet;
}
