
find_package(JNI REQUIRED)

include_directories(
        .
        ../shared
        ${JNI_INCLUDE_DIRS}
)

add_library(bridge SHARED
        ../shared/SharedStructs.hpp
        util/NativeDisplay.cpp
        platform/GameBoy.cpp
        platform/GameBoy.hpp
        util/util.hpp
        util/NativeDisplay.hpp
)

target_link_libraries(bridge Boost::interprocess)
