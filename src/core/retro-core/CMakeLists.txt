
include_directories(.)

add_library(retro-core SHARED
        sys/libretro.h
        sys/LibRetroCore.cpp
        sys/LibRetroCore.hpp
        export.h
        export.cpp
)
