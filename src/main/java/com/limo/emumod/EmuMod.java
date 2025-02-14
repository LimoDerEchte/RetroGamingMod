package com.limo.emumod;

import com.limo.emumod.registry.EmuItems;
import net.fabricmc.api.ModInitializer;

public class EmuMod implements ModInitializer {

    @Override
    public void onInitialize() {
        EmuItems.init();
    }
}
