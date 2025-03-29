package com.limo.emumod.cartridge;

import com.limo.emumod.console.GenericHandheldItem;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import static com.limo.emumod.registry.EmuComponents.FILE_ID;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class LinkedCartridgeItem extends Item {
    public Item linkItem;
    private final String fileType;
    private final Runnable clearLinkItem;
    private final BiFunction<PlayerEntity, UUID, Boolean> start;

    public LinkedCartridgeItem(RegistryKey<Item> key, String fileType, Runnable clearLinkItem, BiFunction<PlayerEntity, UUID, Boolean> start) {
        super(new Settings().maxCount(1).registryKey(key));
        this.fileType = fileType;
        this.clearLinkItem = clearLinkItem;
        this.start = start;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        gameTooltip(stack, tooltip);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient())
            return super.use(world, user, hand);
        ItemStack stack = user.getStackInHand(hand);
        ItemStack link = GenericHandheldItem.link != null && GenericHandheldItem.link.getItem() == linkItem ?
                GenericHandheldItem.link : ItemStack.EMPTY;
        if(link.getCount() > 0 && hasGame(stack)) {
            UUID id = stack.getComponents().get(FILE_ID);
            File file = FileUtil.idToFile(id, fileType);
            if(!file.exists()) {
                stack.setCount(0);
                user.getInventory().insertStack(new ItemStack(EmuItems.BROKEN_CARTRIDGE));
                user.sendMessage(Text.translatable("item.emumod.handheld.file_deleted")
                        .formatted(Formatting.RED), true);
            } else {
                clearLinkItem.run();
                if(start.apply(user, id)) {
                    link.applyComponentsFrom(ComponentMap.builder()
                            .add(GAME, stack.getComponents().get(GAME))
                            .add(FILE_ID, stack.getComponents().get(FILE_ID)).build());
                    stack.setCount(0);
                    user.sendMessage(Text.translatable("item.emumod.handheld.insert"), true);
                }
            }
        }
        return ActionResult.PASS;
    }

    public static void gameTooltip(ItemStack stack, List<Text> tooltip) {
        ComponentMap components = stack.getComponents();
        if(components.contains(FILE_ID) && components.contains(GAME)) {
            tooltip.add(Text.translatable("item.emumod.tooltip.game", components.get(GAME)).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("item.emumod.tooltip.file", components.get(FILE_ID)).formatted(Formatting.GRAY));
        }
    }

    public static boolean hasGame(ItemStack stack) {
        ComponentMap components = stack.getComponents();
        return components.contains(FILE_ID) && components.contains(GAME);
    }
}
