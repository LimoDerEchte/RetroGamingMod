
mkdir build-linux

# Build x64
cmake -S . -B build-linux-x64 -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=toolchain/linux-x86_64.cmake
cmake --build build-linux-x64 --config Release --target bridge retro-core

mv build-linux-x64/bridge/libbridge.so build-linux/libbridge-linux-x64.so
mv build-linux-x64/retro-core/retro-core build-linux/retro-core-linux-x64
