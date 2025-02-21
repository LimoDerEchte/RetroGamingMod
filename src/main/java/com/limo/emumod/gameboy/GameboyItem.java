package com.limo.emumod.gameboy;

import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.registry.ItemId;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;

import static com.limo.emumod.cartridge.LinkedCartridgeItem.FILE_ID;
import static com.limo.emumod.cartridge.LinkedCartridgeItem.GAME;

public class GameboyItem extends Item {
    public static long lastInteractionTime;

    public GameboyItem(RegistryKey<Item> type) {
        super(new Settings().maxCount(1).registryKey(type));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        LinkedCartridgeItem.gameTooltip(stack, tooltip);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return LinkedCartridgeItem.hasGame(stack);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient())
            return super.use(world, user, hand);
        ItemStack stack = user.getStackInHand(hand);
        if(user.isSneaking() && System.currentTimeMillis() > lastInteractionTime + 1000) {
            lastInteractionTime = System.currentTimeMillis();
            if(LinkedCartridgeItem.hasGame(stack)) {
                ItemStack cart = new ItemStack(stack.getItem() ==
                        EmuItems.GAMEBOY_ADVANCE ? EmuItems.GAMEBOY_ADVANCE_CARTRIDGE : stack.getItem() ==
                        EmuItems.GAMEBOY_COLOR ? EmuItems.GAMEBOY_COLOR_CARTRIDGE : EmuItems.GAMEBOY_CARTRIDGE);
                cart.applyComponentsFrom(ComponentMap.builder()
                        .add(GAME, stack.getComponents().get(GAME))
                        .add(FILE_ID, stack.getComponents().get(FILE_ID)).build());
                user.getInventory().insertStack(cart);
                stack.remove(GAME);
                stack.remove(FILE_ID);
                // TODO: Dispose GB
                user.sendMessage(Text.translatable("item.gameboy.eject"), true);
            }
        }
        return ActionResult.PASS;
    }
}
