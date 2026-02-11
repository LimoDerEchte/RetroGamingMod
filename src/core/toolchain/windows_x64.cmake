
set(CMAKE_SYSTEM_NAME Windows)
set(CMAKE_SYSTEM_PROCESSOR x86_64)

# Compiler Options
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER clang)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} --target=x86_64-windows-gnu -fuse-ld=lld")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} --target=x86_64-windows-gnu -fuse-ld=lld")

# Library Options
set(VCPKG_TARGET_TRIPLET x64-mingw-static)
add_definitions(-D_AMD64_ -DWIN32_LEAN_AND_MEAN)

set(JNI_INCLUDE_DIRS
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/share"
    "${CMAKE_CURRENT_LIST_DIR}/../lib/jni/windows")

include(${CMAKE_CURRENT_LIST_DIR}/../vcpkg/scripts/buildsystems/vcpkg.cmake)
