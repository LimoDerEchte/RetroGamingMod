package com.limo.emumod;

import com.limo.emumod.network.C2S;
import com.limo.emumod.network.S2C;
import com.limo.emumod.network.ServerHandler;
import com.limo.emumod.registry.EmuItems;
import net.fabricmc.api.ModInitializer;

public class EmuMod implements ModInitializer {

    @Override
    public void onInitialize() {
        EmuItems.init();
        S2C.init();
        C2S.init();
        ServerHandler.init();
    }
}
