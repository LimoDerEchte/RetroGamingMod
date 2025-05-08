//
// Created by limo on 3/3/25.
//

#pragma once
#include <boost/interprocess/managed_shared_memory.hpp>

#include "SharedStructs.hpp"

namespace bip = boost::interprocess;

namespace GenericConsole {
    int load(bip::managed_shared_memory* mem, const char *core, const char *rom, const char *save);
    void runLoops(GenericShared* shared, const char *save);
}
