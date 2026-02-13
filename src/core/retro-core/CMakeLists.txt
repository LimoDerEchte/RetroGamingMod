
add_executable(retro-core main.cpp
        sys/LibRetroCore.cpp
        platform/GenericConsole.cpp
)

target_include_directories(retro-core PRIVATE
        .
        ../shared
)

target_link_libraries(retro-core PRIVATE Boost::interprocess bridge)

# Post Build
if(NOT CMAKE_BUILD_TYPE STREQUAL "Release")
    if(UNIX)
        add_custom_command(TARGET retro-core POST_BUILD
                COMMAND cp -f ${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/retro-core/retro-core* ${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/
        )
    elseif(WIN32)
        add_custom_command(TARGET retro-core POST_BUILD
                COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/../cmake-build-debug/retro-core/retro-core.exe" "${CMAKE_CURRENT_SOURCE_DIR}/../../../run/retroGamingRequired/"
        )
    endif()
endif()