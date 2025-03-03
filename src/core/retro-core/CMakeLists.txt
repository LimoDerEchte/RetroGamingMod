
include_directories(
        .
        ../shared
)

add_executable(retro-core main.cpp
        ../shared/SharedStructs.hpp
        ../lib/libretro-common/include/libretro.h
        sys/LibRetroCore.cpp
        sys/LibRetroCore.hpp
        platform/GameBoy.cpp
        platform/GameBoy.hpp
)

target_link_libraries(retro-core Boost::interprocess)
