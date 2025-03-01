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
    gameboy.start();
    const auto display = gameboy.getDisplay();
    while (!display->changed && gameboy.child->running()) {}
    gameboy.dispose();
    return 0;
}
