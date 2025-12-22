package com.limo.emumod.data;

import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import com.limo.emumod.registry.EmuItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EmuModDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        EmuComponents.init();
        EmuItems.init();
        EmuBlockEntities.init();

        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(EmuModRecipeProvider::new);

    }

    private static class EmuModRecipeProvider extends FabricRecipeProvider {

        public EmuModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected @NotNull RecipeGenerator getRecipeGenerator(RegistryWrapper.@NotNull WrapperLookup wrapperLookup, @NotNull RecipeExporter recipeExporter) {
            return new RecipeGenerator(wrapperLookup, recipeExporter) {
                @Override
                public void generate() {
                    createShaped(RecipeCategory.REDSTONE, EmuItems.CARTRIDGE, 3)
                            .pattern("IPI")
                            .pattern("IPI")
                            .pattern("CCC")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('P', Items.PAPER)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT));

                    createShapeless(RecipeCategory.REDSTONE, EmuItems.CABLE, 3)
                            .input(Items.COPPER_INGOT, 3)
                            .input(Items.REDSTONE, 2)
                            .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT));

                    createShaped(RecipeCategory.REDSTONE, EmuItems.NES_CONTROLLER)
                            .pattern("BBB")
                            .pattern("III")
                            .pattern("CRC")
                            .input('B', Items.STONE_BUTTON)
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('R', Items.REDSTONE)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT));

                    createShaped(RecipeCategory.REDSTONE, EmuItems.MONITOR)
                            .pattern("IGI")
                            .pattern("GGG")
                            .pattern("III")
                            .input('I', Items.IRON_INGOT)
                            .input('G', Items.TINTED_GLASS)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT));

                    createShaped(RecipeCategory.REDSTONE, EmuItems.LARGE_TV)
                            .pattern("IGI")
                            .pattern("GMG")
                            .pattern("IGI")
                            .input('I', Items.IRON_INGOT)
                            .input('G', Items.TINTED_GLASS)
                            .input('M', EmuItems.MONITOR)
                            .criterion(hasItem(EmuItems.MONITOR), conditionsFromItem(EmuItems.MONITOR));

                    createShaped(RecipeCategory.REDSTONE, EmuItems.GAMEBOY)
                            .pattern("IPI")
                            .pattern("CLC")
                            .pattern("CGC")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('G', Items.GOLD_INGOT)
                            .input('P', Items.TINTED_GLASS)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT));

                    createShapeless(RecipeCategory.REDSTONE, EmuItems.GAMEBOY_COLOR)
                            .input(EmuItems.GAMEBOY)
                            .input(Items.RED_DYE)
                            .input(Items.GREEN_DYE)
                            .input(Items.BLUE_DYE)
                            .criterion(hasItem(EmuItems.GAMEBOY), conditionsFromItem(EmuItems.GAMEBOY));

                    createShaped(RecipeCategory.REDSTONE, EmuItems.GAMEBOY_ADVANCE)
                            .pattern("RCR")
                            .pattern("IGI")
                            .pattern("RCR")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('R', Items.REDSTONE)
                            .input('G', EmuItems.GAMEBOY_COLOR)
                            .criterion(hasItem(EmuItems.GAMEBOY_COLOR), conditionsFromItem(EmuItems.GAMEBOY_COLOR));

                    createShaped(RecipeCategory.REDSTONE, EmuItems.GAME_GEAR)
                            .pattern("III")
                            .pattern("IPI")
                            .pattern("CRC")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('R', Items.REDSTONE)
                            .input('P', Items.PAPER)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT));

                    createShaped(RecipeCategory.REDSTONE, EmuItems.NES)
                            .pattern("RIR")
                            .pattern("CCC")
                            .pattern("RIR")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('R', Items.REDSTONE)
                            .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT));

                    offerSmelting(
                            List.of(EmuItems.BROKEN_CARTRIDGE),
                            RecipeCategory.REDSTONE,
                            EmuItems.CARTRIDGE,
                            0.1f,
                            300,
                            "repair"
                    );
                }
            };
        }

        @Override
        public String getName() {
            return "EmuModRecipeProvider";
        }
    }
}
