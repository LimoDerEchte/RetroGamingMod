
#pragma once
#include <random>

#define PATH_TO_LIB "/home/limo/IdeaProjects/EmulatorModV2/src/core/cmake-build-debug/bridge-instance/bridge-instance"

inline void GenerateID(char* id, const int length = 32) {
    constexpr char charset[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    constexpr int charsetSize = sizeof(charset) - 1;

    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution distrib(0, charsetSize - 1);

    for (int i = 0; i < length; ++i) {
        id[i] = charset[distrib(gen)];
    }
}
