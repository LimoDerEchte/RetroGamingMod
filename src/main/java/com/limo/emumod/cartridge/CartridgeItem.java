package com.limo.emumod.cartridge;

import com.limo.emumod.network.NetworkId;
import com.limo.emumod.network.S2C;
import com.limo.emumod.registry.ItemId;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class CartridgeItem extends Item {

    public CartridgeItem() {
        super(new Properties().stacksTo(8).setId(ItemId.Registry.CARTRIDGE));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if(world.isClientSide())
            return super.use(world, user, hand);
        if(!(user instanceof ServerPlayer player))
            return InteractionResult.PASS;
        ServerPlayNetworking.send(player, new S2C.OpenScreenPayload(NetworkId.ScreenType.CARTRIDGE));
        return InteractionResult.SUCCESS;
    }
}
