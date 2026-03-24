mkdir ../run/retroGamingRequired
cp -f target/debug/libretro_bridge.so ../run/retroGamingRequired/libbridge.so
cp -f target/debug/retro-core ../run/retroGamingRequired/retro-core
echo "Successfully copied debug files"