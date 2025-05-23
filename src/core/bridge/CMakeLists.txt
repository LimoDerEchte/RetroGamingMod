
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
        codec/VideoEncoder.cpp
        codec/VideoEncoder.hpp
        codec/VideoDecoder.cpp
        codec/VideoDecoder.hpp
        codec/AudioEncoder.cpp
        codec/AudioEncoder.hpp
        codec/AudioDecoder.cpp
        codec/AudioDecoder.hpp
        util/AudioSource.cpp
        util/AudioSource.hpp
)

target_include_directories(bridge PRIVATE
        .
        ../shared
        ${JNI_INCLUDE_DIRS}
        ${OPUS_INCLUDE_DIRS}
)
target_link_libraries(bridge PRIVATE Boost::interprocess Boost::process unofficial::enet::enet OpenAL::OpenAL ZLIB::ZLIB ${OPUS_LIBRARIES})

# FFmpeg
#target_include_directories(bridge PRIVATE ${FFMPEG_INCLUDE_DIRS})
#target_link_directories(bridge PRIVATE ${FFMPEG_LIBRARY_DIRS})
#target_link_libraries(bridge PRIVATE ${FFMPEG_LIBRARIES})

# Post Build
add_custom_command(TARGET bridge POST_BUILD
        COMMAND cp -f ${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/bridge/libbridge.* ${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/
)
