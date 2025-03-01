
include_directories(
        .
        ../shared
        ../lib/lrcpp/include
)

add_executable(bridge-instance main.cpp
        platform/GameBoy.cpp
        platform/GameBoy.hpp
        sys/LibRetroCore.cpp
        sys/LibRetroCore.hpp
        sys/paths.h
)
