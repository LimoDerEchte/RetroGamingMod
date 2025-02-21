
# Project Stuff
cmake_minimum_required(VERSION 3.30)
project(RetroGamingCore)

set(CMAKE_CXX_STANDARD 26)

# Include Libs
add_subdirectory(lib/mgba)
add_subdirectory(lib/cppfs)

# Create Core
add_subdirectory(bridge)
target_link_libraries(bridge PRIVATE mgba cppfs)
