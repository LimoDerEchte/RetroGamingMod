
# Project Stuff
cmake_minimum_required(VERSION 3.30)
set(CMAKE_TOOLCHAIN_FILE ${CMAKE_CURRENT_SOURCE_DIR}/vcpkg/scripts/buildsystems/vcpkg.cmake CACHE STRING "Vcpkg toolchain file")

project(RetroGamingCore)
set(CMAKE_CXX_STANDARD 26)

# Lib
find_package(JNI REQUIRED)
find_package(boost_interprocess CONFIG REQUIRED)
find_package(boost_process CONFIG REQUIRED)
find_package(unofficial-enet CONFIG REQUIRED)
find_package(PkgConfig REQUIRED)
pkg_check_modules(dav1d REQUIRED IMPORTED_TARGET dav1d)

# Actual Projects
add_subdirectory(bridge)
add_subdirectory(retro-core)
add_subdirectory(test)
