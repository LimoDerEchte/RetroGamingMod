
#include <iostream>

#include "platform/GenericConsole.hpp"

int main(const int argc, char* argv[]) {
    if (argc < 6) {
        std::cerr << "This should NEVER be called by a user (to few arguments)" << std::endl;
        std::cerr << "Usage: retro-core <platform> <id> <core> <rom> <save>" << std::endl;
        return EXIT_FAILURE;
    }
    const std::string platform(argv[1]);
    const char *id = argv[2];
    const char *core = argv[3];
    const char *rom = argv[4];
    const char *save = argv[5];
    bip::managed_shared_memory *segment;
    try {
        segment = new bip::managed_shared_memory(bip::open_only, id);
    } catch (bip::interprocess_exception &e) {
        std::cerr << "[RetroGamingCore] " << e.what() << std::endl;
        return EXIT_FAILURE;
    }
    if (platform == "gn") {
        return GenericConsole::load(segment, core, rom, save);
    }
    std::cerr << "This should NEVER be called by a user (unknown platform " << platform << ")" << std::endl;
    return EXIT_FAILURE;
}
