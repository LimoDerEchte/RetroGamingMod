
add_library(bridge SHARED
        platform/GenericConsole.cpp
        connection/Server.cpp
        connection/Client.cpp
        connection/NetworkDefinitions.cpp
        util/NativeUtil.cpp
        codec/VideoEncoder.cpp
        codec/VideoDecoder.cpp
        codec/AudioEncoder.cpp
        codec/AudioDecoder.cpp
        util/NativeImage.cpp
)

target_include_directories(bridge PRIVATE
        .
        ../shared
        ${JNI_INCLUDE_DIRS}
        ${OPUS_INCLUDE_DIRS}
        ${VCPKG_INCLUDE_DIRS}
)

target_link_libraries(bridge PRIVATE
        Boost::interprocess
        Boost::process
        unofficial::enet::enet
        fmt::fmt
        PkgConfig::dav1d
        SVT-AV1::SVT-AV1
        yuv
        ${OPUS_LIBRARIES}
)

if(WIN32)
    target_link_libraries(bridge PRIVATE winmm avrt)
endif()

# Post Build
if(NOT CMAKE_BUILD_TYPE STREQUAL "Release")
    if(UNIX)
        add_custom_command(TARGET bridge POST_BUILD
                COMMAND cp -f ${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/bridge/libbridge.* ${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/
        )
    elseif(WIN32)
        add_custom_command(TARGET bridge POST_BUILD
                COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/bridge/bridge.dll" "${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/"
        )
    endif()
endif()
