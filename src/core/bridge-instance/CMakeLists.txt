
include_directories(
        .
        ../shared
)

add_executable(bridge-instance main.cpp
        util/util.hpp
        platform/GameBoy.cpp
        platform/GameBoy.hpp
)
