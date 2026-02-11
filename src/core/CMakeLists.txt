
# Compiling
set(CMAKE_GENERATOR ninja)
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER clang)

# Clang Optimizations
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG -march=native -flto -funroll-loops -fvectorize -fmerge-all-constants -ffast-math -fstrict-aliasing")
endif()

# VCPKG Config
if(UNIX AND NOT APPLE)
    set(VCPKG_TARGET_TRIPLET x64-linux)
else()
    set(VCPKG_TARGET_TRIPLET x64-windows-static)
    add_definitions(-D_AMD64_ -DWIN32_LEAN_AND_MEAN)
endif()
set(CMAKE_FIND_LIBRARY_SUFFIXES ".lib;.a")
set(BUILD_SHARED_LIBS OFF)

# Project Stuff
cmake_minimum_required(VERSION 3.30)
set(CMAKE_TOOLCHAIN_FILE ${CMAKE_CURRENT_SOURCE_DIR}/vcpkg/scripts/buildsystems/vcpkg.cmake CACHE STRING "Vcpkg toolchain file")

project(RetroGamingCore)
set(CMAKE_CXX_STANDARD 23)

# Lib
find_package(boost_interprocess CONFIG REQUIRED)
find_package(boost_process CONFIG REQUIRED)
find_package(unofficial-enet CONFIG REQUIRED)
find_package(Opus CONFIG REQUIRED)
find_package(fmt CONFIG REQUIRED)
find_package(libyuv CONFIG REQUIRED)
find_package(WebP CONFIG REQUIRED)

# Actual Projects
add_subdirectory(bridge)
add_subdirectory(retro-core)
add_subdirectory(test)
