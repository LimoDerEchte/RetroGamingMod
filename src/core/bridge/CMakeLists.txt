
find_package(JNI REQUIRED)

include_directories(${JNI_INCLUDE_DIRS})

include_directories(.)

add_library(bridge SHARED
        util/NativeDisplay.cpp
        util/NativeDisplay.h
)
