//
// Created by limo on 3/9/25.
//

#pragma once

struct jUUID {
    long mostSignificantBits;
    long leastSignificantBits;

    [[nodiscard]] long combine() const;
};

void GenerateID(char* id, int length = 32);
