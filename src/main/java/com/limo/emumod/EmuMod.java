package com.limo.emumod;

import com.limo.emumod.network.C2S;
import com.limo.emumod.network.S2C;
import com.limo.emumod.network.ServerHandler;
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

    static {
        System.load("/home/limo/IdeaProjects/EmulatorModV2/src/core/cmake-build-debug/bridge/libbridge.so");
    }

    @Override
    public void onInitialize() {
        FileUtil.initGeneric();
        CoreManager.init();
        EmuItems.init();
        S2C.init();
        C2S.init();
        ServerHandler.init();
    }
}
