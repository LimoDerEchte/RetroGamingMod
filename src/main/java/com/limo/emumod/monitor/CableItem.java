package com.limo.emumod.monitor;

import com.limo.emumod.console.GenericConsoleBlockEntity;
import com.limo.emumod.registry.ItemId;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import static com.limo.emumod.registry.EmuComponents.CONSOLE_LINK_ID;

public class CableItem extends Item {

    public CableItem() {
        super(new Item.Settings().maxCount(8).registryKey(ItemId.Registry.CABLE));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity user = context.getPlayer();
        World world = context.getWorld();
        if(world.isClient())
            return super.useOnBlock(context);
        if(!(user instanceof ServerPlayerEntity player))
            return ActionResult.PASS;
        ItemStack stack = context.getStack();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        BlockEntity entity = world.getBlockEntity(pos);
        // Link Game to Monitor
        if(entity instanceof MonitorBlockEntity mon) {
            if(!stack.hasChangedComponent(CONSOLE_LINK_ID)) {
                player.sendMessage(Text.translatable("item.emumod.cable.no_link"), true);
                return ActionResult.SUCCESS;
            }
            mon.consoleId = stack.get(CONSOLE_LINK_ID);
            mon.markDirty();
            world.updateListeners(pos, state, state, 0);
            stack.remove(CONSOLE_LINK_ID);
            player.sendMessage(Text.translatable("item.emumod.cable.link"), true);
            return ActionResult.SUCCESS;
        }
        // Read Game from Console
        if(entity instanceof GenericConsoleBlockEntity con) {
            stack.applyComponentsFrom(ComponentMap.builder().add(CONSOLE_LINK_ID, con.consoleId.consoleId()).build());
            player.sendMessage(Text.translatable("item.emumod.cable.read"), true);
        }
        return ActionResult.SUCCESS;
    }
}
