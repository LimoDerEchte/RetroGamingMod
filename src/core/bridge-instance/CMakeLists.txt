
include_directories(
        .
        ../shared
        ../lib/lrcpp/include
)

add_executable(bridge-instance main.cpp
        platform/GameBoy.cpp
        platform/GameBoy.hpp
        sys/Video.hpp
        sys/Video.cpp
)
