package com.limo.emumod.monitor;

import com.limo.emumod.registry.BlockId;
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
import net.minecraft.world.World;

import java.util.Objects;

import static com.limo.emumod.registry.EmuComponents.CONSOLE;

public class MonitorBlock extends BlockWithEntity {
    private static final MapCodec<MonitorBlock> CODEC = Block.createCodec((_) -> new MonitorBlock());

    public MonitorBlock() {
        super(Settings.create()
                .nonOpaque().sounds(BlockSoundGroup.GLASS).emissiveLighting((_, world, pos)
                        -> world.getBlockEntity(pos) instanceof MonitorBlockEntity mon && mon.consoleId != null)
                .pistonBehavior(PistonBehavior.DESTROY).registryKey(BlockId.Registry.MONITOR));
        setDefaultState(this.stateManager.getDefaultState().with(Properties.ROTATION, 0));
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
            ComponentMap comp = stack.getComponents();
            if(comp.contains(CONSOLE)) {
                BlockEntity entity = world.getBlockEntity(pos);
                if(entity instanceof MonitorBlockEntity mon) {
                    mon.consoleId = Objects.requireNonNull(comp.get(CONSOLE)).consoleId();
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
        stateManager.add(Properties.ROTATION);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        ctx.getWorld().markDirty(ctx.getBlockPos());
        return this.getDefaultState().with(Properties.ROTATION, (-Math.round(ctx.getPlayerYaw() / 22.5f) + 16) % 16);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MonitorBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return state.get(Properties.ROTATION) == 0 ? BlockRenderType.MODEL : BlockRenderType.INVISIBLE;
    }
}
