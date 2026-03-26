package com.limo.emumod.monitor;

import com.limo.emumod.EmuMod;
import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

public class MonitorBlockEntity extends BlockEntity {
    public UUID consoleId;

    public MonitorBlockEntity(BlockPos pos, BlockState state) {
        super(EmuBlockEntities.MONITOR, pos, state);
    }

    @Override
    protected void applyImplicitComponents(@NonNull DataComponentGetter components) {
        super.applyImplicitComponents(components);
        consoleId = components.getOrDefault(EmuComponents.CONSOLE_LINK_ID, EmuMod.UUID_ZERO);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NonNull Builder builder) {
        super.collectImplicitComponents(builder);
        if(consoleId != null) {
            builder.set(EmuComponents.CONSOLE_LINK_ID, consoleId);
        }
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput view) {
        if(consoleId != null)
            view.store("file_id", UUIDUtil.AUTHLIB_CODEC, consoleId);

        super.saveAdditional(view);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput view) {
        super.loadAdditional(view);

        consoleId = view.read("file_id", UUIDUtil.AUTHLIB_CODEC).orElse(consoleId);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
