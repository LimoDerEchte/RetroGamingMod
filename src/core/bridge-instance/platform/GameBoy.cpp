//
// Created by limo on 3/1/25.
//

#include "GameBoy.hpp"

#include <iostream>
#include <lrcpp/Frontend.h>

#include "PlatformStructures.hpp"

namespace bip = boost::interprocess;

int GB::init(const std::string &id, bip::managed_shared_memory *segment) {
    auto [shared, size] = segment->find<GameBoyShared>("SharedData");
    if (shared == nullptr || size == 0) {
        std::cerr << "[RetroGamingCore | " << id << "] Error: SharedData not found!" << std::endl;
        return EXIT_FAILURE;
    }
    lrcpp::Frontend *frontend = lrcpp::Frontend::getCurrent();
    frontend->setCore();
    // TODO: Actual gb stuff
    return EXIT_SUCCESS;
}

