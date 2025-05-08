package com.limo.emumod.monitor;

import com.limo.emumod.EmuMod;
import com.limo.emumod.cartridge.LinkedCartridgeItem;
import com.limo.emumod.console.ControllerItem;
import com.limo.emumod.registry.BlockId;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
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
import net.minecraft.world.World;

import static com.limo.emumod.registry.EmuComponents.FILE_ID;

public class MonitorBlock extends BlockWithEntity {
    private static final MapCodec<MonitorBlock> CODEC = Block.createCodec((s) -> new MonitorBlock());

    public MonitorBlock() {
        super(Settings.create()
                .nonOpaque().sounds(BlockSoundGroup.GLASS).emissiveLighting((state, world, pos)
                        -> world.getBlockEntity(pos) instanceof MonitorBlockEntity mon && mon.fileId != null)
                .pistonBehavior(PistonBehavior.DESTROY).registryKey(BlockId.Registry.MONITOR));
        setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(stack.getItem() instanceof CableItem)
            return ActionResult.PASS;
        if(!player.isSneaking()) {
            if(LinkedCartridgeItem.hasGame(stack)) {
                BlockEntity entity = world.getBlockEntity(pos);
                if(entity instanceof MonitorBlockEntity mon) {
                    mon.fileId = stack.get(FILE_ID);
                    mon.markDirty();
                    world.updateListeners(pos, state, state, 0);
                    player.sendMessage(Text.translatable("item.emumod.cable.link"), true);
                }
            }
        }
        return ActionResult.SUCCESS;
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
        return new MonitorBlockEntity(pos, state);
    }
}
