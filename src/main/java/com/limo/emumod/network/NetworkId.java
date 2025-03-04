package com.limo.emumod.network;

import net.minecraft.util.Identifier;

public class NetworkId {
    public static final Identifier OPEN_SCREEN = Identifier.of("emumod", "open_screen");
    public static final Identifier OPEN_GAME_SCREEN = Identifier.of("emumod", "open_game_screen");
    public static final Identifier CLOSE_SCREEN = Identifier.of("emumod", "close_screen");

    public static final Identifier UPDATE_DISPLAY_DATA = Identifier.of("emumod", "display_data");
    public static final Identifier UPDATE_AUDIO_DATA = Identifier.of("emumod", "audio_data");

    public static final Identifier CREATE_CARTRIDGE = Identifier.of("emumod", "create_cartridge");
    public static final Identifier UPDATE_CONTROLS = Identifier.of("emumod", "update_controls");

    public static class ScreenType {
        public static final byte CARTRIDGE = 0x00;

        public static final byte GAMEBOY = 0x00;
        public static final byte GAMEBOY_COLOR = 0x01;
        public static final byte GAMEBOY_ADVANCE = 0x02;
    }

    public static class DisplaySize {
        public static final byte w160h144 = 0x00;
        public static final byte w240h160 = 0x01;
    }
}
