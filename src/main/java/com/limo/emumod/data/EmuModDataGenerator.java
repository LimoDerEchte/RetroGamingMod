package com.limo.emumod.data;

import com.limo.emumod.registry.EmuBlocks;
import com.limo.emumod.registry.EmuItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
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

        protected EmuModBlockLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
            super(dataOutput, registryLookup);
        }

        @Override
        public void generate() {
            dropSelf(EmuBlocks.NES);
            dropSelf(EmuBlocks.MONITOR);
            dropSelf(EmuBlocks.LARGE_TV);
        }
    }

    private static class EmuModRecipeProvider extends FabricRecipeProvider {

        public EmuModRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider wrapperLookup, @NotNull RecipeOutput recipeExporter) {
            return new RecipeProvider(wrapperLookup, recipeExporter) {
                @Override
                public void buildRecipes() {
                    shaped(RecipeCategory.REDSTONE, EmuItems.CARTRIDGE, 3)
                            .pattern("IPI")
                            .pattern("IPI")
                            .pattern("CCC")
                            .define('I', Items.IRON_INGOT)
                            .define('C', Items.COPPER_INGOT)
                            .define('P', Items.PAPER)
                            .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                            .save(recipeExporter);

                    shapeless(RecipeCategory.REDSTONE, EmuItems.CABLE, 3)
                            .requires(Items.COPPER_INGOT, 3)
                            .requires(Items.REDSTONE, 2)
                            .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                            .save(recipeExporter);

                    shaped(RecipeCategory.REDSTONE, EmuItems.NES_CONTROLLER)
                            .pattern("BBB")
                            .pattern("III")
                            .pattern("CRC")
                            .define('B', Items.STONE_BUTTON)
                            .define('I', Items.IRON_INGOT)
                            .define('C', Items.COPPER_INGOT)
                            .define('R', Items.REDSTONE)
                            .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                            .save(recipeExporter);

                    shaped(RecipeCategory.REDSTONE, EmuItems.MONITOR)
                            .pattern("IGI")
                            .pattern("GGG")
                            .pattern("III")
                            .define('I', Items.IRON_INGOT)
                            .define('G', Items.GLASS_PANE)
                            .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                            .save(recipeExporter);

                    shaped(RecipeCategory.REDSTONE, EmuItems.LARGE_TV)
                            .pattern("IGI")
                            .pattern("GMG")
                            .pattern("IGI")
                            .define('I', Items.IRON_INGOT)
                            .define('G', Items.GLASS_PANE)
                            .define('M', EmuItems.MONITOR)
                            .unlockedBy(getHasName(EmuItems.MONITOR), has(EmuItems.MONITOR))
                            .save(recipeExporter);

                    shaped(RecipeCategory.REDSTONE, EmuItems.GAMEBOY)
                            .pattern("ICI")
                            .pattern("BPB")
                            .pattern("ICI")
                            .define('I', Items.IRON_INGOT)
                            .define('C', Items.COPPER_INGOT)
                            .define('B', Items.STONE_BUTTON)
                            .define('P', Items.GLASS_PANE)
                            .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                            .save(recipeExporter);

                    shapeless(RecipeCategory.REDSTONE, EmuItems.GAMEBOY_COLOR)
                            .requires(EmuItems.GAMEBOY)
                            .requires(Items.RED_DYE)
                            .requires(Items.GREEN_DYE)
                            .requires(Items.BLUE_DYE)
                            .unlockedBy(getHasName(EmuItems.GAMEBOY), has(EmuItems.GAMEBOY))
                            .save(recipeExporter);

                    shaped(RecipeCategory.REDSTONE, EmuItems.GAMEBOY_ADVANCE)
                            .pattern("RCR")
                            .pattern("IGI")
                            .pattern("RCR")
                            .define('I', Items.IRON_INGOT)
                            .define('C', Items.COPPER_INGOT)
                            .define('R', Items.REDSTONE)
                            .define('G', EmuItems.GAMEBOY_COLOR)
                            .unlockedBy(getHasName(EmuItems.GAMEBOY_COLOR), has(EmuItems.GAMEBOY_COLOR))
                            .save(recipeExporter);

                    shaped(RecipeCategory.REDSTONE, EmuItems.GAME_GEAR)
                            .pattern("III")
                            .pattern("IPI")
                            .pattern("CRC")
                            .define('I', Items.IRON_INGOT)
                            .define('C', Items.COPPER_INGOT)
                            .define('R', Items.REDSTONE)
                            .define('P', Items.PAPER)
                            .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                            .save(recipeExporter);

                    shaped(RecipeCategory.REDSTONE, EmuItems.NES)
                            .pattern("RIR")
                            .pattern("CCC")
                            .pattern("RIR")
                            .define('I', Items.IRON_INGOT)
                            .define('C', Items.COPPER_INGOT)
                            .define('R', Items.REDSTONE)
                            .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                            .save(recipeExporter);

                    oreSmelting(
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
