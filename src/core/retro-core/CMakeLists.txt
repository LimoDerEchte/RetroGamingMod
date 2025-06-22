
add_executable(retro-core main.cpp
        ../shared/SharedStructs.hpp
        ../lib/libretro-common/include/libretro.h
        sys/LibRetroCore.cpp
        sys/LibRetroCore.hpp
        platform/GenericConsole.cpp
        platform/GenericConsole.hpp
)

target_include_directories(retro-core PRIVATE
        .
        ../shared
)

target_link_libraries(retro-core PRIVATE Boost::interprocess bridge)

# Post Build
if(UNIX)
    add_custom_command(TARGET retro-core POST_BUILD
            COMMAND cp -f ${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/retro-core/retro-core* ${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/
    )
elseif(WIN32)
    add_custom_command(TARGET retro-core POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/retro-core/retro-core.exe" "${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/"
    )
endif()