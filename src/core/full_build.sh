
# Windows Crosscompile
cmake -S . -B build-windows \
  -DCMAKE_SYSTEM_NAME="Windows" \
  -DCMAKE_C_FLAGS="--target=x86_64-windows-gnu -fuse-ld=lld " \
  -DCMAKE_CXX_FLAGS="--target=x86_64-windows-gnu -fuse-ld=lld " \
  -DCMAKE_BUILD_TYPE="Release"
cmake --build build-windows --config Release

exit # temporarily build only windows

# Linux Build
cmake -S . -B build-linux -DCMAKE_BUILD_TYPE=Release
cmake --build build-linux --config Release

