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
    public UUID consoleId;

    public MonitorBlockEntity(BlockPos pos, BlockState state) {
        super(EmuBlockEntities.MONITOR, pos, state);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        consoleId = components.getOrDefault(EmuComponents.CONSOLE_LINK_ID, null);
    }

    @Override
    protected void addComponents(ComponentMap.Builder builder) {
        super.addComponents(builder);
        if(consoleId != null) {
            builder.add(EmuComponents.CONSOLE_LINK_ID, consoleId);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        if(consoleId != null)
            view.put("file_id", Uuids.CODEC, consoleId);

        super.writeData(view);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        consoleId = view.read("file_id", Uuids.CODEC).orElse(consoleId);
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
