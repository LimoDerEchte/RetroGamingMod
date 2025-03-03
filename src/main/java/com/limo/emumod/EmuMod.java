package com.limo.emumod;

import com.limo.emumod.network.C2S;
import com.limo.emumod.network.S2C;
import com.limo.emumod.network.ServerHandler;
import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.CoreManager;
import com.limo.emumod.util.FileUtil;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.math.random.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EmuMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(EmuMod.class);
    public static final Random RANDOM = Random.create();

    @Override
    public void onInitialize() {
        FileUtil.initGeneric();
        CoreManager.init();
        EmuComponents.init();
        EmuItems.init();
        EmuBlockEntities.init();
        S2C.init();
        C2S.init();
        ServerHandler.init();
    }
}
