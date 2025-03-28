
add_executable(bridge_test main.cpp
        new_codec/VideoEncoder.hpp
        new_codec/VideoDecoder.hpp)

target_include_directories(bridge_test PRIVATE
        .
        ../bridge
        ../shared
)

target_link_libraries(bridge_test PRIVATE bridge z)
