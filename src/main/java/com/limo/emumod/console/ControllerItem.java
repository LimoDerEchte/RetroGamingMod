package com.limo.emumod.console;

import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.OptionalInt;

import static com.limo.emumod.registry.EmuComponents.*;

public class ControllerItem extends Item {
    private final int maxPortNum;

    public ControllerItem(RegistryKey<Item> type, int ports) {
        super(new Settings().maxCount(1).registryKey(type));
        this.maxPortNum = ports - 1;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient())
            return ActionResult.PASS;
        ItemStack stack = user.getStackInHand(hand);
        if(!stack.getComponents().contains(PORT_NUM) || !stack.getComponents().contains(LINK_ID)) {
            user.sendMessage(Text.literal("Controller not linked"), true);
            return ActionResult.PASS;
        }
        int port = stack.getComponents().getOrDefault(PORT_NUM, 0);
        if(user.isSneaking() && maxPortNum > 0) {
            port++;
            if(port > maxPortNum)
                port = 0;
            user.sendMessage(Text.literal("Switched to Player " + (port + 1)), true);
            stack.set(PORT_NUM, port);
            return ActionResult.PASS;
        }
        ServerPlayNetworking.send((ServerPlayerEntity) user, new S2C.OpenGameScreenPayload(
                NetworkId.ScreenType.CONTROLLER, stack.getComponents().get(LINK_ID), OptionalInt.of(port)));
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient())
            return ActionResult.PASS;
        if(context.getPlayer() != null && context.getPlayer().isSneaking())
            return ActionResult.PASS;
        ItemStack stack = context.getStack();
        BlockEntity entity = context.getWorld().getBlockEntity(context.getBlockPos());
        if (entity instanceof GenericConsoleBlockEntity con) {
            if(con.fileId == null) {
                context.getPlayer().sendMessage(Text.literal("No cartridge inserted to console"), true);
                return ActionResult.SUCCESS;
            }
            stack.applyComponentsFrom(ComponentMap.builder()
                    .add(LINK_ID, con.fileId)
                    .add(PORT_NUM, 0).build());
            context.getPlayer().sendMessage(Text.literal("Controller linked (Player 1)"), true);
            return ActionResult.SUCCESS;
        }
        return ActionResult.SUCCESS;
    }
}
