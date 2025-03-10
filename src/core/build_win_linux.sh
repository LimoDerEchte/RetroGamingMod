#!/bin/bash

# Create necessary directories
mkdir -p build
cd build || exit
mkdir -p win_x64
mkdir -p win_x86
mkdir -p linux_x64
mkdir -p linux_x86

# Download necessary files
mkdir -p shared
cd shared || exit
if [ ! -d "jdk-21" ]; then
  wget https://download.java.net/openjdk/jdk21/ri/openjdk-21+35_windows-x64_bin.zip
  unzip openjdk-21+35_windows-x64_bin.zip
  rm openjdk-21+35_windows-x64_bin.zip
fi
JAVA="realpath jdk-21/"
cd .. || exit

# Build Windows
echo "Building Windows x64"
cd win_x64 || exit
if [ ! -f "bridge/bridge.dll" ]; then
  x86_64-w64-mingw32-cmake -G "Ninja" \
        -DCMAKE_TOOLCHAIN_FILE=../../vcpkg/scripts/buildsystems/vcpkg.cmake \
        -DJAVA_HOME="$JAVA" \
        -DJAVA_INCLUDE_PATH="$JAVA"/include \
        -DJAVA_INCLUDE_PATH2="$JAVA"/include/win32 \
        -DJAVA_AWT_INCLUDE_PATH="$JAVA"/include/win32 \
        -DJAVA_JVM_LIBRARY="$JAVA"/lib/jvm.lib \
        -DJAVA_AWT_LIBRARY="$JAVA"/lib/jawt.lib \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_SYSTEM_NAME=Windows \
        -DCMAKE_C_COMPILER=x86_64-w64-mingw32-gcc \
        -DCMAKE_CXX_COMPILER=x86_64-w64-mingw32-g++ \
        -DCMAKE_RC_COMPILER=x86_64-w64-mingw32-windres \
        -DVCPKG_TARGET_TRIPLET=x64-mingw-static \
        -DVCPKG_CMAKE_SYSTEM_NAME=MinGW \
        -DCMAKE_FIND_ROOT_PATH=/usr/x86_64-w64-mingw32 \
        ../..
  ninja || echo "Windows x64 build failed, continuing..."
else
  echo "Skipping Windows x64"
fi
cd .. || exit

# Build Linux
echo "Building Linux x64"
cd linux_x64 || exit
if [ ! -f "bridge/libbridge.so" ]; then
  cmake -G "Ninja" \
        -DCMAKE_BUILD_TYPE=Release \
        -DCMAKE_SYSTEM_NAME=Linux \
        ../..
  ninja || echo "Linux x64 build failed, continuing..."
else
  echo "Skipping Linux x64"
fi
cd .. || exit
