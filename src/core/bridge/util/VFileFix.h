//
// Created by limo on 2/21/25.
//

#pragma once
#include <filesystem>
#include <fstream>
#include <mgba-util/vfs.h>

#include "cppfs/FileHandle.h"
#include "cppfs/fs.h"

static VFile* VFileLoadFixed(const char *path) {
    const auto file = cppfs::fs::open(std::string(path));
    const auto size = file.size();
    const auto data = new char[size];
    file.createInputStream()->read(data, size);
    const auto vFile = VFileFromMemory(data, size);
    return vFile;
}
