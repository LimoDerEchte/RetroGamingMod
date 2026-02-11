
# Compiler Options
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER clang)

# Library Options
set(VCPKG_TARGET_TRIPLET x64-linux)

set(JNI_INCLUDE_DIRS
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/share"
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/unix"
)

set(H264_LIB "${CMAKE_CURRENT_LIST_DIR}/../build-linux-x64/vcpkg_installed/x64-linux/lib/libopenh264.a")
set(VCPKG_INCLUDE_DIRS "${CMAKE_CURRENT_LIST_DIR}/../build-linux-x64/vcpkg_installed/x64-linux/include")

include(${CMAKE_CURRENT_LIST_DIR}/../vcpkg/scripts/buildsystems/vcpkg.cmake)
