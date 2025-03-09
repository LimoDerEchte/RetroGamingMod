//
// Created by limo on 3/9/25.
//

#pragma once

struct  jUUID {
    long mostSignificantBits;
    long leastSignificantBits;
};

void GenerateID(char* id, int length = 32);
