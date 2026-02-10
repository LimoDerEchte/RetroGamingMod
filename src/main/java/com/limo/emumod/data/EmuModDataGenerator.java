package com.limo.emumod.data;

import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuBlocks;
import com.limo.emumod.registry.EmuComponents;
import com.limo.emumod.registry.EmuItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
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
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(EmuModBlockLootTableProvider::new);
        pack.addProvider(EmuModRecipeProvider::new);
    }

    private static class EmuModBlockLootTableProvider extends FabricBlockLootTableProvider {

        protected EmuModBlockLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
            super(dataOutput, registryLookup);
        }

        @Override
        public void generate() {
            addDrop(EmuBlocks.NES);
            addDrop(EmuBlocks.MONITOR);
            addDrop(EmuBlocks.LARGE_TV);
        }
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
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                            .offerTo(recipeExporter);

                    createShapeless(RecipeCategory.REDSTONE, EmuItems.CABLE, 3)
                            .input(Items.COPPER_INGOT, 3)
                            .input(Items.REDSTONE, 2)
                            .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT))
                            .offerTo(recipeExporter);

                    createShaped(RecipeCategory.REDSTONE, EmuItems.NES_CONTROLLER)
                            .pattern("BBB")
                            .pattern("III")
                            .pattern("CRC")
                            .input('B', Items.STONE_BUTTON)
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('R', Items.REDSTONE)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                            .offerTo(recipeExporter);

                    createShaped(RecipeCategory.REDSTONE, EmuItems.MONITOR)
                            .pattern("IGI")
                            .pattern("GGG")
                            .pattern("III")
                            .input('I', Items.IRON_INGOT)
                            .input('G', Items.GLASS_PANE)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                            .offerTo(recipeExporter);

                    createShaped(RecipeCategory.REDSTONE, EmuItems.LARGE_TV)
                            .pattern("IGI")
                            .pattern("GMG")
                            .pattern("IGI")
                            .input('I', Items.IRON_INGOT)
                            .input('G', Items.GLASS_PANE)
                            .input('M', EmuItems.MONITOR)
                            .criterion(hasItem(EmuItems.MONITOR), conditionsFromItem(EmuItems.MONITOR))
                            .offerTo(recipeExporter);

                    createShaped(RecipeCategory.REDSTONE, EmuItems.GAMEBOY)
                            .pattern("ICI")
                            .pattern("BPB")
                            .pattern("ICI")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('B', Items.STONE_BUTTON)
                            .input('P', Items.GLASS_PANE)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                            .offerTo(recipeExporter);

                    createShapeless(RecipeCategory.REDSTONE, EmuItems.GAMEBOY_COLOR)
                            .input(EmuItems.GAMEBOY)
                            .input(Items.RED_DYE)
                            .input(Items.GREEN_DYE)
                            .input(Items.BLUE_DYE)
                            .criterion(hasItem(EmuItems.GAMEBOY), conditionsFromItem(EmuItems.GAMEBOY))
                            .offerTo(recipeExporter);

                    createShaped(RecipeCategory.REDSTONE, EmuItems.GAMEBOY_ADVANCE)
                            .pattern("RCR")
                            .pattern("IGI")
                            .pattern("RCR")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('R', Items.REDSTONE)
                            .input('G', EmuItems.GAMEBOY_COLOR)
                            .criterion(hasItem(EmuItems.GAMEBOY_COLOR), conditionsFromItem(EmuItems.GAMEBOY_COLOR))
                            .offerTo(recipeExporter);

                    createShaped(RecipeCategory.REDSTONE, EmuItems.GAME_GEAR)
                            .pattern("III")
                            .pattern("IPI")
                            .pattern("CRC")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('R', Items.REDSTONE)
                            .input('P', Items.PAPER)
                            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                            .offerTo(recipeExporter);

                    createShaped(RecipeCategory.REDSTONE, EmuItems.NES)
                            .pattern("RIR")
                            .pattern("CCC")
                            .pattern("RIR")
                            .input('I', Items.IRON_INGOT)
                            .input('C', Items.COPPER_INGOT)
                            .input('R', Items.REDSTONE)
                            .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT))
                            .offerTo(recipeExporter);

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
