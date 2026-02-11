
cmake -S . -B build-windows-x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=toolchain/windows_x64.cmake
cmake --build build-windows-x64 --config Release
