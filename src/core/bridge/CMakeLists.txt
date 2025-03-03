
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
        util/NativeAudio.cpp
        util/NativeAudio.hpp
)

target_link_libraries(bridge Boost::interprocess)

# Post Build
add_custom_command(TARGET bridge POST_BUILD
        COMMAND cp -f ${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/bridge/libbridge.* ${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/
)
