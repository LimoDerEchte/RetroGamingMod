package com.limo.emumod.console;

import com.limo.emumod.EmuMod;
import com.limo.emumod.components.ConsoleComponent;
import com.limo.emumod.components.GameComponent;
import com.limo.emumod.network.S2C;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.*;

import static com.limo.emumod.network.ServerEvents.mcs;
import static com.limo.emumod.registry.EmuComponents.CONSOLE;
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
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        return super.getTooltipData(stack);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.getComponents().contains(GAME);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient())
            return super.use(world, user, hand);
        ItemStack stack = user.getStackInHand(hand);

        ComponentMap comp = stack.getComponents();
        if(!comp.contains(CONSOLE))
            stack.applyComponentsFrom(ComponentMap.builder().add(CONSOLE, new ConsoleComponent(UUID.randomUUID())).build());

        if(user.isSneaking() && System.currentTimeMillis() > lastInteractionTime + 1000) {
            lastInteractionTime = System.currentTimeMillis();

            if(comp.contains(GAME)) {
                GameComponent game = Objects.requireNonNull(comp.get(GAME));

                ItemStack cart = new ItemStack(cartridgeType);
                cart.applyComponentsFrom(ComponentMap.builder().add(GAME, stack.getComponents().get(GAME)).build());
                user.getInventory().insertStack(cart);

                UUID file = game.fileId();
                if(EmuMod.running.containsKey(file))
                    EmuMod.running.get(file).stop();
                EmuMod.running.remove(game.fileId());

                stack.remove(GAME);
                user.sendMessage(Text.translatable("item.emumod.handheld.eject"), true);
                PlayerLookup.all(mcs).forEach(player ->
                        ServerPlayNetworking.send(player, new S2C.UpdateEmulatorPayload(Objects.requireNonNull(stack
                                .getComponents().get(CONSOLE)).consoleId(), 0, 0, 0, 0)));
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
            if(!stack.hasChangedComponent(GAME)) {
                user.sendMessage(Text.translatable("item.emumod.handheld.no_game"), true);
                return ActionResult.PASS;
            }
            ServerPlayNetworking.send((ServerPlayerEntity) user, new S2C.OpenGameScreenPayload(screenType,
                    Objects.requireNonNull(stack.getComponents().get(CONSOLE)).consoleId(), OptionalInt.empty()));
        }
        return ActionResult.PASS;
    }
}
