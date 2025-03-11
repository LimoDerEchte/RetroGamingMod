
add_library(bridge SHARED
        ../shared/SharedStructs.hpp
        util/NativeDisplay.cpp
        platform/GenericConsole.cpp
        platform/GenericConsole.hpp
        util/NativeDisplay.hpp
        util/NativeAudio.cpp
        util/NativeAudio.hpp
        connection/Server.cpp
        connection/Server.hpp
        connection/Client.cpp
        connection/Client.hpp
        connection/NetworkDefinitions.hpp
        connection/NetworkDefinitions.cpp
        util/NativeUtil.cpp
        util/NativeUtil.hpp
)

target_include_directories(bridge PRIVATE
        .
        ../shared
        ${JNI_INCLUDE_DIRS}
)

target_link_libraries(bridge PRIVATE Boost::interprocess Boost::process unofficial::enet::enet PkgConfig::dav1d)

# Post Build
add_custom_command(TARGET bridge POST_BUILD
        COMMAND cp -f ${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/bridge/libbridge.* ${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/
)
