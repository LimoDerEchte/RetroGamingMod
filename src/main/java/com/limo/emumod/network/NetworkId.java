package com.limo.emumod.network;

import net.minecraft.util.Identifier;

public class NetworkId {
    public static final Identifier OPEN_SCREEN = Identifier.of("emumod", "open_screen");
    public static final Identifier OPEN_GAME_SCREEN = Identifier.of("emumod", "open_game_screen");
    public static final Identifier CLOSE_SCREEN = Identifier.of("emumod", "close_screen");
    public static final Identifier UPDATE_DISPLAY_DATA = Identifier.of("emumod", "display_data");

    public static final Identifier CREATE_CARTRIDGE = Identifier.of("emumod", "create_cartridge");

    public static class ScreenType {
        public static final byte CARTRIDGE_CREATION = 0x00;
    }
}
