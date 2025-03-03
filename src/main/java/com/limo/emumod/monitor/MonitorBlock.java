package com.limo.emumod.monitor;

import com.limo.emumod.EmuMod;
import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.registry.BlockId;
import com.limo.emumod.registry.EmuComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MonitorBlock extends BlockWithEntity {
    private static final MapCodec<MonitorBlock> CODEC = Block.createCodec((s) -> new MonitorBlock());

    public MonitorBlock() {
        super(Settings.create()
                .nonOpaque().sounds(BlockSoundGroup.GLASS)
                .pistonBehavior(PistonBehavior.DESTROY).registryKey(BlockId.Registry.MONITOR));
        setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!player.isSneaking()) {
            if(LinkedCartridgeItem.hasGame(stack)) {
                BlockEntity entity = world.getBlockEntity(pos);
                if(entity instanceof MonitorBlockEntity mon) {
                    mon.fileId = stack.get(LinkedCartridgeItem.FILE_ID);
                    mon.markDirty();
                    world.updateListeners(pos, state, state, 0);
                    EmuMod.LOGGER.info("Linked Monitor at X:{}, Y:{}, Z:{}", pos.getX(), pos.getY(), pos.getZ());
                    player.sendMessage(Text.literal("Monitor linked to game"), true);
                } else
                    player.sendMessage(Text.literal("Internal Error"), true);
            }
        }
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

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MonitorBlockEntity(pos, state);
    }
}
