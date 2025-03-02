//
// Created by limo on 3/1/25.
//

#include <iostream>
#include <platform/GameBoy.hpp>

#define log(msg) std::cout << "[Test] " << msg << std::endl

int main() {
    GameBoy gameboy{};
    gameboy.load("/home/limo/Documents/Test/Links Awakening.gb", "/home/limo/Downloads/mgba_libretro.so");
    gameboy.start();
    const auto display = gameboy.getDisplay();
    while (!display->changed) {}
    gameboy.dispose();
    return 0;
}
