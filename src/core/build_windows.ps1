# Create output directory
New-Item -ItemType Directory -Path build-windows -Force | Out-Null

# Build x64
cmake -S . -B build-windows-x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE="toolchain/windows_x64.cmake" -G Ninja
cmake --build build-windows-x64 --config Release --target retro-core

Move-Item -Path "build-windows-x64/bridge/bridge.dll" -Destination "build-windows/bridge-windows-x64.dll" -Force
Move-Item -Path "build-windows-x64/retro-core/retro-core.exe" -Destination "build-windows/retro-core-windows-x64.exe" -Force
