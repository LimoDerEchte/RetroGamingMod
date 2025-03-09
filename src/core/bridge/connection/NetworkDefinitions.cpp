//
// Created by limo on 3/8/25.
//

#include "NetworkDefinitions.hpp"

#include <cstring>
#include <iostream>

jUUID packetParseUUID(const void *ptr) {
    auto res = jUUID();
    memcpy(&res, ptr, sizeof(jUUID));
    return res;
}

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
    int8_t* packed[data.size() + 9]{};
    const auto sizeB = reinterpret_cast<int8_t*>(data.size());
    memcpy(&packed[0], &type, 1);
    memcpy(&packed[1], &sizeB, sizeof(size_t));
    memcpy(&packed[9], &data[0], data.size());
    return enet_packet_create(packed, sizeof(packed), ENET_PACKET_FLAG_RELIABLE);
}

Int8ArrayPacket::Int8ArrayPacket(const PacketType type, const unsigned char *ptr, const size_t size) : type(type) {
    data.assign(ptr, ptr + size);
}

Int8ArrayPacket* Int8ArrayPacket::unpack(const ENetPacket *packet) {
    const auto type = static_cast<PacketType>(packet->data[0]);
    if (packet->dataLength < 9) {
        std::cerr << "[RetroPacket] Int8arr packet too small (" << type << ")" << std::endl;
        return nullptr;
    }
    size_t size = 0;
    memcpy(&size, &packet->data[1], sizeof(size_t));
    return new Int8ArrayPacket(type, &packet->data[9], size);
}

ENetPacket* Int8ArrayPacket::pack() const {
    int8_t* packed[data.size() + 9]{};
    const auto sizeB = reinterpret_cast<int8_t*>(data.size());
    memcpy(&packed[0], &type, 1);
    memcpy(&packed[1], &sizeB, sizeof(size_t));
    memcpy(&packed[9], &data[0], data.size());
    return enet_packet_create(packed, sizeof(packed), 0);
}

Int16ArrayPacket::Int16ArrayPacket(const PacketType type, const jUUID ref, const unsigned char *ptr, const size_t size) : type(type), ref(ref) {
    data.assign(ptr, ptr + size);
}

Int16ArrayPacket * Int16ArrayPacket::unpack(const ENetPacket *packet) {
    const auto type = static_cast<PacketType>(packet->data[0]);
    if (packet->dataLength < 73) {
        std::cerr << "[RetroPacket] Int16arr packet too small (" << type << ")" << std::endl;
        return nullptr;
    }
    size_t size = 0;
    memcpy(&size, &packet->data[65], sizeof(size_t));
    return new Int16ArrayPacket(type, packetParseUUID(&packet->data[1]), &packet->data[73], size);
}

ENetPacket * Int16ArrayPacket::pack() const{
    int8_t* packed[data.size() * 2 + 73]{};
    const auto sizeB = reinterpret_cast<int16_t*>(data.size());
    memcpy(&packed[0], &type, 1);
    memcpy(&packed[1], &ref, sizeof(jUUID));
    memcpy(&packed[65], &sizeB, sizeof(size_t));
    memcpy(&packed[73], &data[0], data.size() * 2);
    return enet_packet_create(packed, sizeof(packed), 0);
}
