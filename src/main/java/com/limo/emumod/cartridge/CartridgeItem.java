package com.limo.emumod.cartridge;

import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import com.limo.emumod.registry.ItemId;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class CartridgeItem extends Item {

    public CartridgeItem() {
        super(new Settings().maxCount(8).registryKey(ItemId.Registry.CARTRIDGE));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient)
            return super.use(world, user, hand);
        if(!(user instanceof ServerPlayerEntity player))
            return ActionResult.PASS;
        ServerPlayNetworking.send(player, new S2C.OpenScreenPayload(NetworkId.ScreenType.CARTRIDGE_CREATION));
        return ActionResult.SUCCESS;
    }
}
