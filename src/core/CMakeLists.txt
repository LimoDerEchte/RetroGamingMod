
# Stuff
set(CFLAGS -fsanitize=address -fno-omit-frame-pointer)
set(CXXFLAGS -fsanitize=address -fno-omit-frame-pointer)
set(LDFLAGS -fsanitize=address)

# VCPKG Config
set(VCPKG_TARGET_TRIPLET x64-linux)
set(CMAKE_FIND_LIBRARY_SUFFIXES ".lib;.a")
set(BUILD_SHARED_LIBS OFF)

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
find_package(Opus CONFIG REQUIRED)
find_package(OpenAL REQUIRED openal)

# Actual Projects
add_subdirectory(bridge)
add_subdirectory(retro-core)
add_subdirectory(test)

# Statically include CXX
if(UNIX AND NOT APPLE)
    target_link_options(bridge PRIVATE
            "-static-libgcc"
            "-static-libstdc++"
    )
    target_link_options(retro-core PRIVATE
            "-static-libgcc"
            "-static-libstdc++"
    )
endif()
