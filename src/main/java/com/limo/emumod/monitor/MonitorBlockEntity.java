package com.limo.emumod.monitor;

import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MonitorBlockEntity extends BlockEntity {
    public UUID fileId;

    public MonitorBlockEntity(BlockPos pos, BlockState state) {
        super(EmuBlockEntities.MONITOR, pos, state);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        fileId = components.getOrDefault(EmuComponents.LINK_ID, null);
    }

    @Override
    protected void addComponents(ComponentMap.Builder builder) {
        super.addComponents(builder);
        if(fileId != null) {
            builder.add(EmuComponents.LINK_ID, fileId);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        if(fileId != null)
            view.put("file_id", Uuids.CODEC, fileId);

        super.writeData(view);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        fileId = view.read("file_id", Uuids.CODEC).orElse(fileId);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }
}
