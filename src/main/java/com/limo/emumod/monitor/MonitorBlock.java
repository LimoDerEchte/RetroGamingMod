package com.limo.emumod.monitor;

import com.limo.emumod.registry.BlockId;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class MonitorBlock extends HorizontalFacingBlock {
    private static final MapCodec<MonitorBlock> CODEC = Block.createCodec((s) -> new MonitorBlock());

    public MonitorBlock() {
        super(Settings.create()
                .nonOpaque().sounds(BlockSoundGroup.GLASS)
                .pistonBehavior(PistonBehavior.DESTROY).registryKey(BlockId.Registry.MONITOR));
        setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        /* Deprecated link code
        if(!player.isSneaking()) {
            ItemStack stack = player.getStackInHand(hand);
            if(stack.hasNbt() && stack.getNbt().contains("emumod.fileid")) {
                NbtCompound nbt = new NbtCompound();
                nbt.put("linkID", stack.getNbt().get("emumod.fileid"));
                world.getBlockEntity(pos).readNbt(nbt);
                EmuMod.LOGGER.info("Linked Monitor at X:" + pos.getX() + ", Y:" + pos.getY() + ", Z:" + pos.getZ() + " to " + nbt.get("linkID"));
            }
        } */
        return ActionResult.SUCCESS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0.4F, 0.0F, 0.4F, 0.6F, 0.6F, 0.6F);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
}
