# Retro Gaming Mod
## FAQ
### What is this?
This project aims at adding multiple playable retro consoles to Minecraft. What consoles are supported is listed below.
### How do I install this?
It's as simple as downloading the jar. All necessary libraries are downloaded on-the-fly. ROMs have to be supplied by the user.
### Where do the emulators run?
The emulators all run fully on the server and sync display and audio data to clients. This allows for watching other players play.
### How was this made possible?
My previous attempt at this used coffee gb, a Java Game Boy emulator, which was kinda inefficient. Now I am using a custom core using JNI and do all emulation through an executable instancing libretro cores. This allows me to implement a wide variety of retro consoles.

## Emulators
### Handheld
| Console         | Core                                                                      | Features     |
|-----------------|---------------------------------------------------------------------------|--------------|
| Gameboy         | [Gearboy](https://github.com/drhelius/Gearboy)                            | Video, Audio |
| Gameboy Color   | [Gearboy](https://github.com/drhelius/Gearboy)                            | Video, Audio |
| Gameboy Advance | [Beetle GBA](https://github.com/libretro/beetle-gba-libretro/tree/master) | Video, Audio |
| SEGA Game Gear  | [Genesis-Plus-GX](https://github.com/ekeeke/Genesis-Plus-GX)              | Video, Audio |

### Stationary
None as of now

## Building / Contributing
### First of all
I strongly discourage contributing currently, as the project is still in its earliest stage and the codebase might change by a lot.
### Getting the project set up
Initially, please clone this repo and all it's submodules and create the run/retroGamingRequired folder.
To now use this project, you first have to use your C++ IDE of choice to open the src/core folder containing the cpp code. If you don't want to edit any native code, you can also just build using cmake.
Afterward you can use your Java IDE of choice to open the gradle project. Now you should be all set.
