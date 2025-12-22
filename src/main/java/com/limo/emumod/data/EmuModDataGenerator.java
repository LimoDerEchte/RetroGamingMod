package com.limo.emumod.data;

import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import com.limo.emumod.registry.EmuItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class EmuModDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        EmuComponents.init();
        EmuItems.init();
        EmuBlockEntities.init();

        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        // TODO
    }
}
