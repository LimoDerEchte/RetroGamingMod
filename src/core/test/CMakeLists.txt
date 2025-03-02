
include_directories(
        .
        ../bridge
        ../shared
)

add_executable(bridge_test main.cpp)

target_link_libraries(bridge_test PRIVATE bridge)
