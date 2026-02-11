
# Compiler Options
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER clang)

# Library Options
set(VCPKG_TARGET_TRIPLET x64-linux)

set(JNI_INCLUDE_DIRS
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/share"
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/unix"
)

include(${CMAKE_CURRENT_LIST_DIR}/../vcpkg/scripts/buildsystems/vcpkg.cmake)
