//
// Created by limo on 2/21/25.
//

#pragma once
#include <filesystem>
#include <fstream>
#include <mgba-util/vfs.h>

#include "cppfs/FileHandle.h"
#include "cppfs/fs.h"

#define val const auto

static VFile* VFileLoadFixed(const char *path) {
    val file = cppfs::fs::open(std::string(path));
    val size = file.size();
    val data = new char[size];
    file.createInputStream()->read(data, size);
    val vFile = VFileFromMemory(data, size);
    return vFile;
}
