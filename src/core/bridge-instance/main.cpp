//
// Created by limo on 3/1/25.
//

#include <iostream>

#include <boost/interprocess/exceptions.hpp>

#include "platform/GameBoy.hpp"

namespace bip = boost::interprocess;

int main(const int argc, char* argv[]) {
    if (argc < 3) {
        std::cerr << "This should never be run by a user!" << std::endl;
        return EXIT_FAILURE;
    }
    const std::string consoleStr = argv[1];
    const std::string idStr = argv[2];
    if (consoleStr.length() < 2 || idStr.length() != 32) {
        std::cerr << "This should never be run by a user!" << std::endl;
        return EXIT_FAILURE;
    }
    try {
        auto segment = bip::managed_shared_memory(bip::open_only, idStr.c_str());
        if (consoleStr == "gb")
            return GB::init(idStr, &segment);
        std::cerr << "A console named \"" << consoleStr << "\" could not be found!" << std::endl;
    } catch (bip::interprocess_exception e) {
        std::cerr << "[BridgeInstance | " << idStr << "] " << e.what() << std::endl;
    }
    return EXIT_FAILURE;
}
