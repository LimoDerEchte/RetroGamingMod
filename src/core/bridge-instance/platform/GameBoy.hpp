//
// Created by limo on 3/1/25.
//

#pragma once
#include <boost/interprocess/managed_shared_memory.hpp>

namespace GB {
    int init(const std::string &id, boost::interprocess::managed_shared_memory *segment);
}
