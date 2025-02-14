package com.limo.emumod.client.network;

import com.limo.emumod.client.screen.CartridgeScreen;
import com.limo.emumod.network.S2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientHandler {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(S2C.OpenScreenPayload.ID, (payload, ctx) -> {
            ctx.client().execute(() -> ctx.client().setScreen(new CartridgeScreen()));
        });
    }
}
