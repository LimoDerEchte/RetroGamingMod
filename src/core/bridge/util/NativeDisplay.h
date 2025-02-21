//
// Created by limo on 2/21/25.
//

#pragma once

struct NativeDisplay {
    bool changed{};
    int *buf{};
    size_t bufSize{};
};
