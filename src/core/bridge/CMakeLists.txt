
find_package(JNI REQUIRED)

include_directories(
        .
        ../shared
        ${JNI_INCLUDE_DIRS}
)

add_library(bridge SHARED
        ../shared/SharedStructs.hpp
        util/NativeDisplay.cpp
        platform/GenericConsole.cpp
        platform/GenericConsole.hpp
        util/util.hpp
        util/NativeDisplay.hpp
        util/NativeAudio.cpp
        util/NativeAudio.hpp
        connection/Server.cpp
        connection/Server.hpp
        connection/Client.cpp
        connection/Client.hpp
        connection/NetworkDefinitions.hpp
        connection/NetworkDefinitions.cpp
)

target_link_libraries(bridge Boost::interprocess unofficial::enet::enet)

# Post Build
add_custom_command(TARGET bridge POST_BUILD
        COMMAND cp -f ${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/bridge/libbridge.* ${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/
)
