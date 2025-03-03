//
// Created by limo on 3/1/25.
//

#include <iostream>
#include <platform/GameBoy.hpp>

#define log(msg) std::cout << "[Test] " << msg << std::endl

int main() {
    GameBoy gameboy(false);
    GameBoy gameboy2(false);
    gameboy.load(
        "/home/limo/IdeaProjects/EmulatorModV2/src/core/cmake-build-debug/retro-core/retro-core",
        "/home/limo/Downloads/mgba_libretro.so",
        "/home/limo/Documents/Test/Links Awakening.gb"
    );
    gameboy2.load(
        "/home/limo/IdeaProjects/EmulatorModV2/src/core/cmake-build-debug/retro-core/retro-core",
        "/home/limo/Downloads/mgba_libretro.so",
        "/home/limo/Documents/Test/Links Awakening.gb"
    );
    const auto display = gameboy.getDisplay();
    const auto display2 = gameboy2.getDisplay();
    while (true) {}
    gameboy.dispose();
    gameboy2.dispose();
    return 0;
}
