package com.limo.emumod.monitor;

import com.limo.emumod.registry.BlockId;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

import static com.limo.emumod.registry.EmuComponents.CONSOLE;

public class MonitorBlock extends BaseEntityBlock {
    private static final MapCodec<MonitorBlock> CODEC = Block.simpleCodec((_) -> new MonitorBlock());

    public MonitorBlock() {
        super(Properties.of()
                .noOcclusion().sound(SoundType.GLASS).emissiveRendering((_, world, pos)
                        -> world.getBlockEntity(pos) instanceof MonitorBlockEntity mon && mon.consoleId != null)
                .pushReaction(PushReaction.DESTROY).setId(BlockId.Registry.MONITOR));
        registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.ROTATION_16, 0));
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NonNull InteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state, Level world, @NonNull BlockPos pos,
                                                   @NonNull Player player, @NonNull InteractionHand hand, @NonNull BlockHitResult hit) {
        if(world.isClientSide())
            return InteractionResult.PASS;
        if(stack.getItem() instanceof CableItem)
            return InteractionResult.PASS;
        if(!player.isShiftKeyDown()) {
            DataComponentMap comp = stack.getComponents();
            if(comp.has(CONSOLE)) {
                BlockEntity entity = world.getBlockEntity(pos);
                if(entity instanceof MonitorBlockEntity mon) {
                    mon.consoleId = Objects.requireNonNull(comp.get(CONSOLE)).consoleId();
                    mon.setChanged();
                    world.sendBlockUpdated(pos, state, state, 0);
                    player.displayClientMessage(Component.translatable("item.emumod.cable.link"), true);
                    return InteractionResult.SUCCESS_SERVER;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateManager) {
        stateManager.add(BlockStateProperties.ROTATION_16);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        ctx.getLevel().blockEntityChanged(ctx.getClickedPos());
        return this.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, (-Math.round(ctx.getRotation() / 22.5f) + 32) % 16);
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new MonitorBlockEntity(pos, state);
    }

    @Override
    protected @NonNull RenderShape getRenderShape(BlockState state) {
        return state.getValue(BlockStateProperties.ROTATION_16) == 0 ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }
}
