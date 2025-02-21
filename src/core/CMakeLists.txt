
cmake_minimum_required(VERSION 3.30)
project(RetroGamingCore)

set(CMAKE_CXX_STANDARD 26)

add_library(vbam-core STATIC)

target_sources(vbam-core
        PRIVATE
        lib/visualboyadvance-m/src/core/gb/internal/gbCodes.h
        lib/visualboyadvance-m/src/core/gb/internal/gbCodesCB.h
        lib/visualboyadvance-m/src/core/gb/gb.cpp
        lib/visualboyadvance-m/src/core/gb/gbCartData.cpp
        lib/visualboyadvance-m/src/core/gb/gbCheats.cpp
        lib/visualboyadvance-m/src/core/gb/gbDis.cpp
        lib/visualboyadvance-m/src/core/gb/gbGfx.cpp
        lib/visualboyadvance-m/src/core/gb/gbGlobals.cpp
        lib/visualboyadvance-m/src/core/gb/gbMemory.cpp
        lib/visualboyadvance-m/src/core/gb/gbPrinter.cpp
        lib/visualboyadvance-m/src/core/gb/gbSGB.cpp
        lib/visualboyadvance-m/src/core/gb/gbSound.cpp
        lib/visualboyadvance-m/src/core/gba/debugger-expr-lex.cpp
        lib/visualboyadvance-m/src/core/gba/debugger-expr-yacc.cpp
        lib/visualboyadvance-m/src/core/gba/gba.cpp
        lib/visualboyadvance-m/src/core/gba/gbaCpuArm.cpp
        lib/visualboyadvance-m/src/core/gba/gbaCpuArmDis.cpp
        lib/visualboyadvance-m/src/core/gba/gbaCpuThumb.cpp
        lib/visualboyadvance-m/src/core/gba/gbaCheats.cpp
        lib/visualboyadvance-m/src/core/gba/gbaCheatSearch.cpp
        lib/visualboyadvance-m/src/core/gba/gbaEeprom.cpp
        lib/visualboyadvance-m/src/core/gba/gbaElf.cpp
        lib/visualboyadvance-m/src/core/gba/gbaFlash.cpp
        lib/visualboyadvance-m/src/core/gba/gbaGfx.cpp
        lib/visualboyadvance-m/src/core/gba/gbaGlobals.cpp
        lib/visualboyadvance-m/src/core/gba/gbaMode0.cpp
        lib/visualboyadvance-m/src/core/gba/gbaMode1.cpp
        lib/visualboyadvance-m/src/core/gba/gbaMode2.cpp
        lib/visualboyadvance-m/src/core/gba/gbaMode3.cpp
        lib/visualboyadvance-m/src/core/gba/gbaMode4.cpp
        lib/visualboyadvance-m/src/core/gba/gbaMode5.cpp
        lib/visualboyadvance-m/src/core/gba/gbaPrint.cpp
        lib/visualboyadvance-m/src/core/gba/gbaRtc.cpp
        lib/visualboyadvance-m/src/core/gba/gbaSound.cpp
        lib/visualboyadvance-m/src/core/gba/internal/gbaBios.cpp
        lib/visualboyadvance-m/src/core/gba/internal/gbaBios.h
        lib/visualboyadvance-m/src/core/gba/internal/gbaEreader.cpp
        lib/visualboyadvance-m/src/core/gba/internal/gbaEreader.h
        lib/visualboyadvance-m/src/core/gba/internal/gbaSram.cpp
        lib/visualboyadvance-m/src/core/gba/internal/gbaSram.h

        PUBLIC
        # Game Boy
        lib/visualboyadvance-m/src/core/gb/gb.h
        lib/visualboyadvance-m/src/core/gb/gbCartData.h
        lib/visualboyadvance-m/src/core/gb/gbCheats.h
        lib/visualboyadvance-m/src/core/gb/gbDis.h
        lib/visualboyadvance-m/src/core/gb/gbGfx.h
        lib/visualboyadvance-m/src/core/gb/gbGlobals.h
        lib/visualboyadvance-m/src/core/gb/gbMemory.h
        lib/visualboyadvance-m/src/core/gb/gbPrinter.h
        lib/visualboyadvance-m/src/core/gb/gbSGB.h
        lib/visualboyadvance-m/src/core/gb/gbSound.h

        # Game Boy Advance
        lib/visualboyadvance-m/src/core/gba/gba.h
        lib/visualboyadvance-m/src/core/gba/gbaCheats.h
        lib/visualboyadvance-m/src/core/gba/gbaCheatSearch.h
        lib/visualboyadvance-m/src/core/gba/gbaCpu.h
        lib/visualboyadvance-m/src/core/gba/gbaCpuArmDis.h
        lib/visualboyadvance-m/src/core/gba/gbaEeprom.h
        lib/visualboyadvance-m/src/core/gba/gbaElf.h
        lib/visualboyadvance-m/src/core/gba/gbaFlash.h
        lib/visualboyadvance-m/src/core/gba/gbaGfx.h
        lib/visualboyadvance-m/src/core/gba/gbaGlobals.h
        lib/visualboyadvance-m/src/core/gba/gbaInline.h
        lib/visualboyadvance-m/src/core/gba/gbaPrint.h
        lib/visualboyadvance-m/src/core/gba/gbaRtc.h
        lib/visualboyadvance-m/src/core/gba/gbaSound.h
)

include_directories(
        lib/visualboyadvance-m/third_party/include/nonstd
        lib/visualboyadvance-m/third_party/include/stb
        lib/visualboyadvance-m/src)

add_subdirectory(bridge)

target_link_libraries(bridge PUBLIC vbam-core)
