package com.limo.emumod.gameboy;

import com.limo.emumod.bridge.NativeGenericConsole;
import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import com.limo.emumod.registry.EmuItems;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.limo.emumod.registry.EmuComponents.FILE_ID;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class GameboyItem extends Item {
    public static long lastInteractionTime;
    public static final Map<UUID, NativeGenericConsole> running = new HashMap<>();
    public static ItemStack link;

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
                UUID file = stack.getComponents().get(FILE_ID);
                if(running.containsKey(file))
                    running.get(file).stop();
                running.remove(stack.getComponents().get(FILE_ID));
                stack.remove(GAME);
                stack.remove(FILE_ID);
                user.sendMessage(Text.translatable("item.emumod.gameboy.eject"), true);
            } else {
                if(link != null) {
                    link = null;
                    user.sendMessage(Text.translatable("item.emumod.gameboy.cancel_link"), true);
                } else {
                    link = user.getStackInHand(hand);
                    user.sendMessage(Text.translatable("item.emumod.gameboy.start_link"), true);
                }
            }
        } else {
            if(!stack.hasChangedComponent(FILE_ID)) {
                user.sendMessage(Text.translatable("item.emumod.gameboy.no_game"), true);
                return ActionResult.PASS;
            }
            ServerPlayNetworking.send((ServerPlayerEntity) user, new S2C.OpenGameScreenPayload(
                    stack.getItem() == EmuItems.GAMEBOY_ADVANCE ? NetworkId.ScreenType.GAMEBOY_ADVANCE :
                    stack.getItem() == EmuItems.GAMEBOY_COLOR ? NetworkId.ScreenType.GAMEBOY_COLOR :
                    NetworkId.ScreenType.GAMEBOY, stack.getComponents().get(FILE_ID)
            ));
        }
        return ActionResult.PASS;
    }
}
