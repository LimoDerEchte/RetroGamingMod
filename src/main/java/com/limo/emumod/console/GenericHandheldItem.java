package com.limo.emumod.console;

import com.limo.emumod.EmuMod;
import com.limo.emumod.components.ConsoleComponent;
import com.limo.emumod.network.S2C;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.*;

import static com.limo.emumod.network.ServerEvents.mcs;
import static com.limo.emumod.registry.EmuComponents.CONSOLE;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class GenericHandheldItem extends Item {
    public static long lastInteractionTime;
    public static ItemStack link;
    private final byte screenType;
    private final Item cartridgeType;

    public GenericHandheldItem(ResourceKey<Item> type, byte screenType, Item cartridgeType) {
        super(new Properties().stacksTo(1).setId(type));
        this.screenType = screenType;
        this.cartridgeType = cartridgeType;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return super.getTooltipImage(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getComponents().has(GAME);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if(world.isClientSide())
            return super.use(world, user, hand);
        ItemStack stack = user.getItemInHand(hand);

        DataComponentMap comp = stack.getComponents();
        if(!comp.has(CONSOLE))
            stack.applyComponents(DataComponentMap.builder().set(CONSOLE, new ConsoleComponent(UUID.randomUUID())).build());
        UUID console = Objects.requireNonNull(comp.get(CONSOLE)).consoleId();

        if(user.isShiftKeyDown() && System.currentTimeMillis() > lastInteractionTime + 1000) {
            lastInteractionTime = System.currentTimeMillis();

            if(comp.has(GAME)) {
                ItemStack cart = new ItemStack(cartridgeType);
                cart.applyComponents(DataComponentMap.builder().set(GAME, stack.getComponents().get(GAME)).build());
                user.getInventory().add(cart);

                if(EmuMod.running.containsKey(console)) {
                    EmuMod.running.get(console).stop();
                    int id = EmuMod.running.remove(console).getId();

                    PlayerLookup.all(mcs).forEach(player -> ServerPlayNetworking.send(player,
                            new S2C.UpdateEmulatorPayload(console, id, 0, 0, 0, 0, 0)));
                }

                stack.remove(GAME);
                user.displayClientMessage(Component.translatable("item.emumod.handheld.eject"), true);
            } else {
                if(link != null) {
                    link = null;
                    user.displayClientMessage(Component.translatable("item.emumod.handheld.cancel_link"), true);
                } else {
                    link = user.getItemInHand(hand);
                    user.displayClientMessage(Component.translatable("item.emumod.handheld.start_link"), true);
                }
            }
        } else {
            if(!stack.hasNonDefault(GAME)) {
                user.displayClientMessage(Component.translatable("item.emumod.handheld.no_game"), true);
                return InteractionResult.PASS;
            }

            if(!EmuMod.running.containsKey(console)) {
                user.displayClientMessage(Component.translatable("item.emumod.handheld.not_running"), true);
                return InteractionResult.PASS;
            }

            ServerPlayNetworking.send((ServerPlayer) user, new S2C.OpenGameScreenPayload(
                    screenType, EmuMod.running.get(console).getId(), OptionalInt.empty()));
        }
        return InteractionResult.PASS;
    }
}
