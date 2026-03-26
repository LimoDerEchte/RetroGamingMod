package com.limo.emumod.monitor;

import com.limo.emumod.console.GenericConsoleBlockEntity;
import com.limo.emumod.registry.ItemId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.limo.emumod.registry.EmuComponents.CONSOLE_LINK_ID;

public class CableItem extends Item {

    public CableItem() {
        super(new Item.Properties().stacksTo(8).setId(ItemId.Registry.CABLE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player user = context.getPlayer();
        Level world = context.getLevel();
        if(world.isClientSide())
            return super.useOn(context);
        if(!(user instanceof ServerPlayer player))
            return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        BlockEntity entity = world.getBlockEntity(pos);
        // Link Game to Monitor
        if(entity instanceof MonitorBlockEntity mon) {
            if(!stack.hasNonDefault(CONSOLE_LINK_ID)) {
                player.displayClientMessage(Component.translatable("item.emumod.cable.no_link"), true);
                return InteractionResult.SUCCESS;
            }
            mon.consoleId = stack.get(CONSOLE_LINK_ID);
            mon.setChanged();
            world.sendBlockUpdated(pos, state, state, 0);
            stack.remove(CONSOLE_LINK_ID);
            player.displayClientMessage(Component.translatable("item.emumod.cable.link"), true);
            return InteractionResult.SUCCESS;
        }
        // Read Game from Console
        if(entity instanceof GenericConsoleBlockEntity con) {
            stack.applyComponents(DataComponentMap.builder().set(CONSOLE_LINK_ID, con.consoleId.consoleId()).build());
            player.displayClientMessage(Component.translatable("item.emumod.cable.read"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
