package com.limo.emumod.console;

import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.ContainerComponent;
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

public class GenericConsoleBlockEntity extends BlockEntity {
    public UUID fileId;
    public ContainerComponent cartridge;

    public GenericConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(EmuBlockEntities.GENERIC_CONSOLE, pos, state);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        fileId = components.getOrDefault(EmuComponents.LINK_ID, null);
        cartridge = components.getOrDefault(EmuComponents.CARTRIDGE, ContainerComponent.DEFAULT);
    }

    @Override
    protected void addComponents(ComponentMap.Builder builder) {
        super.addComponents(builder);
        if(fileId != null)
            builder.add(EmuComponents.LINK_ID, fileId);
        if(cartridge != null)
            builder.add(EmuComponents.CARTRIDGE, cartridge);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        if(fileId != null)
            view.put("file_id", Uuids.CODEC, fileId);
        view.put("cartridge", ContainerComponent.CODEC, cartridge);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        fileId = view.read("file_id", Uuids.CODEC).orElse(null);
        cartridge = view.read("cartridge", ContainerComponent.CODEC).orElse(ContainerComponent.DEFAULT);
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
