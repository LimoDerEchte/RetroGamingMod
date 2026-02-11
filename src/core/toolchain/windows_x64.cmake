
# Compiler Options
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER clang)

# Library Options
set(VCPKG_TARGET_TRIPLET x64-windows-static)
add_definitions(-D_AMD64_ -DWIN32_LEAN_AND_MEAN)

set(JNI_INCLUDE_DIRS
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/share"
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/windows"
)

include(${CMAKE_CURRENT_LIST_DIR}/../vcpkg/scripts/buildsystems/vcpkg.cmake)
