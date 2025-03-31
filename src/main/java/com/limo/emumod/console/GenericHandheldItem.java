package com.limo.emumod.console;

import com.limo.emumod.EmuMod;
import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.network.S2C;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import static com.limo.emumod.network.ServerHandler.mcs;
import static com.limo.emumod.registry.EmuComponents.FILE_ID;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class GenericHandheldItem extends Item {
    public static long lastInteractionTime;
    public static ItemStack link;
    private final byte screenType;
    private final Item cartridgeType;

    public GenericHandheldItem(RegistryKey<Item> type, byte screenType, Item cartridgeType) {
        super(new Settings().maxCount(1).registryKey(type));
        this.screenType = screenType;
        this.cartridgeType = cartridgeType;
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
                ItemStack cart = new ItemStack(cartridgeType);
                cart.applyComponentsFrom(ComponentMap.builder()
                        .add(GAME, stack.getComponents().get(GAME))
                        .add(FILE_ID, stack.getComponents().get(FILE_ID)).build());
                user.getInventory().insertStack(cart);
                UUID file = stack.getComponents().get(FILE_ID);
                if(EmuMod.running.containsKey(file))
                    EmuMod.running.get(file).stop();
                EmuMod.running.remove(stack.getComponents().get(FILE_ID));
                stack.remove(GAME);
                stack.remove(FILE_ID);
                user.sendMessage(Text.translatable("item.emumod.handheld.eject"), true);
                PlayerLookup.all(mcs).forEach(player ->
                        ServerPlayNetworking.send(player, new S2C.UpdateEmulatorPayload(file, 0, 0, 0)));
            } else {
                if(link != null) {
                    link = null;
                    user.sendMessage(Text.translatable("item.emumod.handheld.cancel_link"), true);
                } else {
                    link = user.getStackInHand(hand);
                    user.sendMessage(Text.translatable("item.emumod.handheld.start_link"), true);
                }
            }
        } else {
            if(!stack.hasChangedComponent(FILE_ID)) {
                user.sendMessage(Text.translatable("item.emumod.handheld.no_game"), true);
                return ActionResult.PASS;
            }
            ServerPlayNetworking.send((ServerPlayerEntity) user, new S2C.OpenGameScreenPayload(screenType, stack.getComponents().get(FILE_ID), OptionalInt.empty()));
        }
        return ActionResult.PASS;
    }
}
