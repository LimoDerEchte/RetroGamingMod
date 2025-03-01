//
// Created by limo on 3/1/25.
//

#include <iostream>
#include <platform/GameBoy.hpp>

#define log(msg) std::cout << msg << std::endl

int main() {
    GameBoy gameboy{};
    log(gameboy.id);
    gameboy.allocate("/home/limo/Documents/Test/Links Awakening.gb");
    log("1");
    gameboy.start();
    log("2");
    const auto display = gameboy.getDisplay();
    while (!display->changed) {}
    gameboy.dispose();
    return 0;
}
