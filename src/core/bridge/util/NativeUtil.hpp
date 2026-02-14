
#pragma once

struct jUUID {
    long mostSignificantBits;
    long leastSignificantBits;

    [[nodiscard]] long combine() const;
};

void GenerateID(char* id, int length = 32);
