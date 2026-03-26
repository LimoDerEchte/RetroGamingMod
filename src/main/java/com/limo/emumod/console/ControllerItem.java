package com.limo.emumod.console;

import com.limo.emumod.EmuMod;
import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.OptionalInt;
import java.util.UUID;

import static com.limo.emumod.registry.EmuComponents.*;

public class ControllerItem extends Item {
    private final int maxPortNum;

    public ControllerItem(ResourceKey<Item> type, int ports) {
        super(new Properties().stacksTo(1).setId(type));
        this.maxPortNum = ports - 1;
    }

    // TODO: Localization

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if(world.isClientSide())
            return InteractionResult.PASS;

        ItemStack stack = user.getItemInHand(hand);
        if(!stack.getComponents().has(PORT_NUM) || !stack.getComponents().has(CONSOLE_LINK_ID)) {
            user.displayClientMessage(Component.translatable("item.emumod.controller.no_link"), true);
            return InteractionResult.PASS;
        }

        int port = stack.getComponents().getOrDefault(PORT_NUM, 0);
        if(user.isShiftKeyDown() && maxPortNum > 0) {
            port++;
            if(port > maxPortNum)
                port = 0;
            user.displayClientMessage(Component.translatable("item.emumod.controller.switch", port + 1), true);
            stack.set(PORT_NUM, port);
            return InteractionResult.SUCCESS_SERVER;
        }

        UUID console = stack.getComponents().get(CONSOLE_LINK_ID);
        if(!EmuMod.running.containsKey(console)) {
            user.displayClientMessage(Component.translatable("item.emumod.controller.no_link"), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        ServerPlayNetworking.send((ServerPlayer) user, new S2C.OpenGameScreenPayload(
                NetworkId.ScreenType.CONTROLLER, EmuMod.running.get(console).getId(), OptionalInt.of(port)));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel().isClientSide())
            return InteractionResult.PASS;
        if(context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        BlockEntity entity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (entity instanceof GenericConsoleBlockEntity con) {
            stack.applyComponents(DataComponentMap.builder()
                    .set(CONSOLE_LINK_ID, con.consoleId.consoleId())
                    .set(PORT_NUM, 0).build());

            context.getPlayer().displayClientMessage(Component.translatable("item.emumod.controller.linked"), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
