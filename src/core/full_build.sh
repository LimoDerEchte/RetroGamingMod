
# Windows Crosscompile
cmake -S . -B build-windows -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=toolchain/windows_x64.cmake
cmake --build build-windows --config Release

exit # temporarily build only windows

# Linux Build
cmake -S . -B build-linux -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=toolchain/linux_x64.cmake
cmake --build build-linux --config Release

