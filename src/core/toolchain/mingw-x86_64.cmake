
# Compiler Options
set(CMAKE_SYSTEM_NAME Windows)
set(CMAKE_SYSTEM_PROCESSOR x86_64)
set(CMAKE_CROSSCOMPILING ON)

set(CMAKE_GENERATOR Ninja)
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER clang)

set(CMAKE_CXX_COMPILER_TARGET x86_64-w64-windows-gnu)
set(CMAKE_C_COMPILER_TARGET x86_64-w64-windows-gnu)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fuse-ld=lld")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fuse-ld=lld")

set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static -static-libgcc -static-libstdc++")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static-libgcc -static-libstdc++")

set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)

# Library Options
set(VCPKG_TARGET_TRIPLET x64-mingw-static)
set(VCPKG_APPLOCAL_DEPS OFF)
add_definitions(-D_AMD64_ -DWIN32_LEAN_AND_MEAN)

set(JNI_INCLUDE_DIRS
        "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/share"
        "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/windows"
)

set(VCPKG_INCLUDE_DIRS "${CMAKE_CURRENT_LIST_DIR}/../build-windows-x64/vcpkg_installed/x64-windows-static/include")
include(${CMAKE_CURRENT_LIST_DIR}/../vcpkg/scripts/buildsystems/vcpkg.cmake)
