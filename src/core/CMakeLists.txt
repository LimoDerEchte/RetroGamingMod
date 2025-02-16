
cmake_minimum_required(VERSION 3.30)
project(RetroGamingCore)

set(CMAKE_CXX_STANDARD 26)

include_directories(
        lib/visualboyadvance-m/third_party/include/nonstd
        lib/visualboyadvance-m/third_party/include/stb
        lib/visualboyadvance-m/src)

add_subdirectory(lib/visualboyadvance-m/src/core)
add_subdirectory(bridge)

target_link_libraries(bridge PUBLIC vbam-core)
