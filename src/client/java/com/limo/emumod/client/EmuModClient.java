package com.limo.emumod.client;

import com.limo.emumod.client.network.ClientHandler;
import net.fabricmc.api.ClientModInitializer;

public class EmuModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientHandler.init();
    }
}
