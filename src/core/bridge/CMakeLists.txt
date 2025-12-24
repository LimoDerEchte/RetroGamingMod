
add_library(bridge SHARED
        ../shared/SharedStructs.hpp
        platform/GenericConsole.cpp
        platform/GenericConsole.hpp
        connection/Server.cpp
        connection/Server.hpp
        connection/Client.cpp
        connection/Client.hpp
        connection/NetworkDefinitions.hpp
        connection/NetworkDefinitions.cpp
        util/NativeUtil.cpp
        util/NativeUtil.hpp
        codec/VideoEncoder.cpp
        codec/VideoEncoder.hpp
        codec/VideoDecoder.cpp
        codec/VideoDecoder.hpp
        codec/AudioEncoder.cpp
        codec/AudioEncoder.hpp
        codec/AudioDecoder.cpp
        codec/AudioDecoder.hpp
        util/NativeImage.cpp
        util/NativeImage.hpp
)

target_include_directories(bridge PRIVATE
        .
        ../shared
        ${JNI_INCLUDE_DIRS}
        ${OPUS_INCLUDE_DIRS}
)

target_link_libraries(bridge PRIVATE
        Boost::interprocess
        Boost::process
        unofficial::enet::enet
        ZLIB::ZLIB
        fmt::fmt
        openh264
        ${OPUS_LIBRARIES}
)

if(WIN32)
    target_link_libraries(bridge PRIVATE winmm avrt)
endif()

# FFmpeg
#target_include_directories(bridge PRIVATE ${FFMPEG_INCLUDE_DIRS})
#target_link_directories(bridge PRIVATE ${FFMPEG_LIBRARY_DIRS})
#target_link_libraries(bridge PRIVATE ${FFMPEG_LIBRARIES})

# Post Build
if(UNIX)
    add_custom_command(TARGET bridge POST_BUILD
            COMMAND cp -f ${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/bridge/libbridge.* ${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/
    )
elseif(WIN32)
    add_custom_command(TARGET bridge POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/bridge/bridge.dll" "${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/"
    )
endif()