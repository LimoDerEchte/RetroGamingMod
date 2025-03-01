
include_directories(lrcpp/include)

add_library(lrcpp SHARED
        lrcpp/src/Components.cpp
        lrcpp/src/CoreFsm.cpp
        lrcpp/src/Frontend.cpp
)
