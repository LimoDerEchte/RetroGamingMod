
# Project Stuff
cmake_minimum_required(VERSION 3.30)
project(RetroGamingCore)

set(CMAKE_CXX_STANDARD 26)

# Library Stuff (vcpkg & internal)
find_package(boost_interprocess CONFIG REQUIRED)

# Actual Stuff & Linking
add_subdirectory(bridge)
target_link_libraries(bridge PRIVATE Boost::interprocess)

add_subdirectory(bridge-instance)
#target_link_libraries(bridge-instance PRIVATE Boost::interprocess)
