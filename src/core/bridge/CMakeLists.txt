
find_package(JNI REQUIRED)

include_directories(
        .
        ${JNI_INCLUDE_DIRS}
)

add_library(bridge SHARED
        util/NativeDisplay.cpp
        util/NativeDisplay.hpp
        platform/GameBoy.cpp
        platform/GameBoy.hpp
        util/util.hpp
)
