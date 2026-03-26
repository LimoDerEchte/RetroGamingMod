package com.limo.emumod.console;

import com.limo.emumod.EmuMod;
import com.limo.emumod.bridge.NativeServer;
import com.limo.emumod.network.S2C;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.function.TriFunction;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.limo.emumod.network.ServerEvents.mcs;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class GenericConsoleBlock extends BaseEntityBlock {
    private final MapCodec<GenericConsoleBlock> CODEC;
    private final Item cartridgeItem;
    private final String fileType;
    private final TriFunction<Player, UUID, UUID, Boolean> start;

    public GenericConsoleBlock(ResourceKey<Block> registryKey, Item cartridgeItem, String fileType, TriFunction<Player, UUID, UUID, Boolean> start) {
        super(Properties.of().noOcclusion().sound(SoundType.METAL)
                .pushReaction(PushReaction.DESTROY).setId(registryKey));
        this.cartridgeItem = cartridgeItem;
        this.fileType = fileType;
        this.start = start;
        registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        CODEC = Block.simpleCodec((s) -> new GenericConsoleBlock(registryKey, cartridgeItem, fileType, start));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(world.isClientSide() || !(world.getBlockEntity(pos) instanceof GenericConsoleBlockEntity con) || !hand.equals(InteractionHand.MAIN_HAND))
            return InteractionResult.TRY_WITH_EMPTY_HAND;

        UUID console = con.consoleId.consoleId();
        if(console != null && player.isShiftKeyDown()) {
            if(EmuMod.running.containsKey(console))
                EmuMod.running.get(console).stop();
            int id = EmuMod.running.remove(console).getId();

            PlayerLookup.all(mcs).forEach(sp -> ServerPlayNetworking.send(sp,
                    new S2C.UpdateEmulatorPayload(console, id, 0, 0, 0, 0, 0)));

            player.getInventory().add(con.cartridge.copyOne());
            con.cartridge = ItemContainerContents.EMPTY;
            con.fileId = null;
            con.setChanged();

            player.displayClientMessage(Component.translatable("item.emumod.handheld.eject"), true);
        }

        DataComponentMap components = stack.getComponents();
        if(stack.getItem() != cartridgeItem || !components.has(GAME))
            return InteractionResult.TRY_WITH_EMPTY_HAND;

        UUID id = Objects.requireNonNull(components.get(GAME)).fileId();
        File file = FileUtil.idToFile(id, fileType);
        if(!file.exists()) {
            stack.setCount(0);
            player.getInventory().add(new ItemStack(EmuItems.BROKEN_CARTRIDGE));
            player.displayClientMessage(Component.translatable("item.emumod.handheld.file_deleted").withStyle(ChatFormatting.RED), true);
        } else {
            if(start.apply(player, id, con.consoleId.consoleId())) {
                con.fileId = id;
                con.cartridge = ItemContainerContents.fromItems(List.of(stack.copy()));
                con.setChanged();
                stack.setCount(0);
                player.displayClientMessage(Component.translatable("item.emumod.handheld.insert"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateManager) {
        stateManager.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GenericConsoleBlockEntity(pos, state);
    }
}
