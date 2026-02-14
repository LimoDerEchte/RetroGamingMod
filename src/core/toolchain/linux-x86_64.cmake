
# Compiler Options
set(CMAKE_GENERATOR Ninja)
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER clang)

set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -fuse-ld=lld")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -fuse-ld=lld")

# Library Options
set(VCPKG_TARGET_TRIPLET x64-linux)

set(JNI_INCLUDE_DIRS
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/share"
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/unix"
)

set(VCPKG_INCLUDE_DIRS "${CMAKE_CURRENT_LIST_DIR}/../build-linux-x64/vcpkg_installed/x64-linux/include")
include(${CMAKE_CURRENT_LIST_DIR}/../vcpkg/scripts/buildsystems/vcpkg.cmake)
