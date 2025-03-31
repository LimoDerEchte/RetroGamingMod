package com.limo.emumod.console;

import com.limo.emumod.EmuMod;
import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.network.S2C;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.io.File;
import java.util.UUID;
import java.util.function.BiFunction;

import static com.limo.emumod.network.ServerHandler.mcs;
import static com.limo.emumod.registry.EmuComponents.FILE_ID;

public class GenericConsoleBlock extends BlockWithEntity {
    private final MapCodec<GenericConsoleBlock> CODEC;
    private final Item cartridgeItem;
    private final String fileType;
    private final BiFunction<PlayerEntity, UUID, Boolean> start;

    public GenericConsoleBlock(RegistryKey<Block> registryKey, Item cartridgeItem, String fileType, BiFunction<PlayerEntity, UUID, Boolean> start) {
        super(Settings.create().nonOpaque().sounds(BlockSoundGroup.METAL)
                .pistonBehavior(PistonBehavior.DESTROY).registryKey(registryKey));
        this.cartridgeItem = cartridgeItem;
        this.fileType = fileType;
        this.start = start;
        setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        CODEC = Block.createCodec((s) -> new GenericConsoleBlock(registryKey, cartridgeItem, fileType, start));
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient || !(world.getBlockEntity(pos) instanceof GenericConsoleBlockEntity con))
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        if(con.fileId != null && player.isSneaking()) {
            if(EmuMod.running.containsKey(con.fileId))
                EmuMod.running.get(con.fileId).stop();
            EmuMod.running.remove(con.fileId);
            player.getInventory().insertStack(con.cartridge);
            PlayerLookup.all(mcs).forEach(sp ->
                ServerPlayNetworking.send(sp, new S2C.UpdateEmulatorPayload(con.fileId, 0, 0, 0)));
            con.cartridge = null;
            con.fileId = null;
            con.markDirty();
            player.sendMessage(Text.translatable("item.emumod.handheld.eject"), true);
        }
        if(stack.getItem() != cartridgeItem || !LinkedCartridgeItem.hasGame(stack))
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        UUID id = stack.getComponents().get(FILE_ID);
        File file = FileUtil.idToFile(id, fileType);
        if(!file.exists()) {
            stack.setCount(0);
            player.getInventory().insertStack(new ItemStack(EmuItems.BROKEN_CARTRIDGE));
            player.sendMessage(Text.translatable("item.emumod.handheld.file_deleted")
                    .formatted(Formatting.RED), true);
        } else {
            if(start.apply(player, id)) {
                con.fileId = id;
                con.cartridge = stack.copy();
                con.markDirty();
                stack.setCount(0);
                player.sendMessage(Text.translatable("item.emumod.handheld.insert"), true);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GenericConsoleBlockEntity(pos, state);
    }
}
