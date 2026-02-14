
mkdir build-mingw

# Build x64
cmake -S . -B build-mingw-x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=toolchain/mingw-x86_64.cmake
cmake --build build-mingw-x64 --config Release --target bridge retro-core

mv build-mingw-x64/bridge/libbridge.dll build-mingw/bridge-mingw-x64.dll
mv build-mingw-x64/retro-core/retro-core.exe build-mingw/retro-core-mingw-x64.exe
