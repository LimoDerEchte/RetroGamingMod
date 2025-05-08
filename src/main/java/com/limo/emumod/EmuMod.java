package com.limo.emumod;

import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.bridge.NativeServer;
import com.limo.emumod.network.C2S;
import com.limo.emumod.network.S2C;
import com.limo.emumod.network.ServerHandler;
import com.limo.emumod.network.ServerEvents;
import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.RequirementManager;
import com.limo.emumod.util.FileUtil;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.math.random.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EmuMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(EmuMod.class);
    public static final Random RANDOM = Random.create();
    public static final UUID UUID_ZERO = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static final Map<UUID, NativeGenericConsole> running = new HashMap<>();
    public static NativeServer SERVER;

    @Override
    public void onInitialize() {
        FileUtil.initGeneric();
        RequirementManager.init();
        EmuComponents.init();
        EmuItems.init();
        EmuBlockEntities.init();
        S2C.init();
        C2S.init();
        ServerHandler.init();
        ServerEvents.init();
        System.load(FileUtil.getRequiredFile("libbridge.so").getAbsolutePath());
    }
}
