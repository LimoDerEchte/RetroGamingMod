
#include <iostream>

#include "platform/GameBoy.hpp"

int main(const int argc, char* argv[]) {
    if (argc < 5) {
        std::cerr << "This should NEVER be called by a user (to few arguments)" << std::endl;
        return EXIT_FAILURE;
    }
    std::string platform(argv[1]);
    const char *id = argv[2];
    const char *core = argv[3];
    const char *rom = argv[4];
    bip::managed_shared_memory *segment;
    try {
        segment = new bip::managed_shared_memory(bip::open_only, id);
    } catch (bip::interprocess_exception &e) {
        std::cerr << "[RetroGamingCore] " << e.what() << std::endl;
        return EXIT_FAILURE;
    }
    if (platform == "gb") {
        return GB::load(segment, core, rom);
    }
    std::cerr << "This should NEVER be called by a user (unknown platform " << platform << ")" << std::endl;
    return EXIT_FAILURE;
}
