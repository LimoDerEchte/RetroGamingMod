package com.limo.emumod.client.network;

import com.limo.emumod.client.screen.CartridgeScreen;
import com.limo.emumod.client.screen.GameboyAdvanceScreen;
import com.limo.emumod.client.screen.GameboyScreen;
import com.limo.emumod.network.S2C;
import com.limo.emumod.screen.ScreenId;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.limo.emumod.client.EmuModClient.mc;

public class ClientHandler {
    public static Map<UUID, int[]> displayBuffer = new HashMap<>();

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenScreenPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> ctx.client().setScreen(switch (payload.type()) {
            case ScreenId.CARTRIDGE -> new CartridgeScreen();
            default -> throw new AssertionError();
        })));
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenGameScreenPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> ctx.client().setScreen(switch (payload.type()) {
                    case ScreenId.GAMEBOY -> new GameboyScreen(false, payload.fileId());
                    case ScreenId.GAMEBOY_COLOR -> new GameboyScreen(true, payload.fileId());
                    case ScreenId.GAMEBOY_ADVANCE -> new GameboyAdvanceScreen(payload.fileId());
                    default -> throw new AssertionError();
        })));
        ClientPlayNetworking.registerGlobalReceiver(S2C.CloseScreenPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
            if(mc.currentScreen instanceof CartridgeScreen screen && screen.handle == payload.handle())
                screen.close();
        }));
        ClientPlayNetworking.registerGlobalReceiver(S2C.UpdateDisplayDataPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
            displayBuffer.put(payload.uuid(), payload.data());
        }));
    }
}
