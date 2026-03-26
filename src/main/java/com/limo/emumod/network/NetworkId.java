package com.limo.emumod.network;

import net.minecraft.resources.Identifier;

public class NetworkId {
    public static final Identifier ENET_TOKEN = Identifier.fromNamespaceAndPath("emumod", "enet_token");

    public static final Identifier OPEN_SCREEN = Identifier.fromNamespaceAndPath("emumod", "open_screen");
    public static final Identifier OPEN_GAME_SCREEN = Identifier.fromNamespaceAndPath("emumod", "open_game_screen");
    public static final Identifier CLOSE_SCREEN = Identifier.fromNamespaceAndPath("emumod", "close_screen");

    public static final Identifier UPDATE_EMULATOR = Identifier.fromNamespaceAndPath("emumod", "update_emu");
    public static final Identifier UPDATE_AUDIO = Identifier.fromNamespaceAndPath("emumod", "update_audio");

    public static final Identifier CREATE_CARTRIDGE = Identifier.fromNamespaceAndPath("emumod", "create_cartridge");

    public static class ScreenType {
        public static final byte CARTRIDGE          = 0x00;

        public static final byte GAMEBOY            = 0x00;
        public static final byte GAMEBOY_COLOR      = 0x01;
        public static final byte GAMEBOY_ADVANCE    = 0x02;
        public static final byte GAME_GEAR          = 0x03;

        public static final byte CONTROLLER         = 0x40;
    }
}
