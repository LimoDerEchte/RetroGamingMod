package com.limo.emumod.client.network;

import com.limo.emumod.client.screen.CartridgeScreen;
import com.limo.emumod.network.S2C;
import com.limo.emumod.screen.ScreenId;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import static com.limo.emumod.client.EmuModClient.mc;

public class ClientHandler {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenScreenPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> ctx.client().setScreen(switch (payload.type()) {
            case ScreenId.CARTRIDGE -> new CartridgeScreen();
            default -> throw new AssertionError();
        })));
        ClientPlayNetworking.registerGlobalReceiver(S2C.CloseScreenPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> {
            if(mc.currentScreen instanceof CartridgeScreen screen && screen.handle == payload.handle())
                screen.close();
        }));
    }
}
