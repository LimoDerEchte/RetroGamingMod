
find_package(JNI REQUIRED)

include_directories(
        .
        ../shared
        ${JNI_INCLUDE_DIRS}
)

add_library(bridge SHARED
        util/NativeDisplay.cpp
        platform/GameBoy.cpp
        platform/GameBoy.hpp
        util/util.hpp
        ../shared/PlatformStructures.hpp
        ../shared/MainStructures.hpp
)
