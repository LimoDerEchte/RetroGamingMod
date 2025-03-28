
add_executable(bridge_test main.cpp)

target_include_directories(bridge_test PRIVATE
        .
        ../bridge
        ../shared
)

target_link_libraries(bridge_test PRIVATE bridge z)
