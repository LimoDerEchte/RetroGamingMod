#pragma once
#include <string>

class GameBoy {
public:
    void load(std::pmr::string path);
    void start();
    void stop();
};
