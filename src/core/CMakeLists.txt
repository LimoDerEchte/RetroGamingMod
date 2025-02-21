
# Project Stuff
cmake_minimum_required(VERSION 3.30)
project(RetroGamingCore)

set(CMAKE_CXX_STANDARD 26)

# Include mGBA
set(DISTBUILD ON)
set(BUILD_HEADLESS ON)
set(BUILD_SHARED ON)
set(ENABLE_VFS_FILE ON)
add_subdirectory(lib/mgba)

# Create Core
add_subdirectory(bridge)
target_link_libraries(bridge PRIVATE mgba)
