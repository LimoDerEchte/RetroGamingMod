//
// Created by limo on 3/1/25.
//

#include <iostream>
#include <platform/GenericConsole.hpp>

#define log(msg) std::cout << "[Test] " << msg << std::endl

int main() {
    GenericConsole gameboy(160, 144, new jUUID{0,0});
    GenericConsole gameboy2(160, 144, new jUUID{1,1});
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
