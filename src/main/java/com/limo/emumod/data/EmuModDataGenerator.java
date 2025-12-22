package com.limo.emumod.data;

import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import com.limo.emumod.registry.EmuItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.NotNull;

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

                    createShaped(RecipeCategory.REDSTONE, EmuItems.GAMEBOY)
                            .pattern("IPI")
                            .pattern("CLC")
                            .pattern("CGC")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('G', Items.GOLD_INGOT)
                            .input('P', Items.GLASS_PANE)
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
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT));
                }
            };
        }

        @Override
        public String getName() {
            return "EmuModRecipeProvider";
        }
    }
}
