package com.limo.emumod.console;

import com.limo.emumod.registry.EmuBlockEntities;
import com.limo.emumod.registry.EmuComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GenericConsoleBlockEntity extends BlockEntity {
    public UUID fileId;
    public ItemStack cartridge;

    public GenericConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(EmuBlockEntities.GENERIC_CONSOLE, pos, state);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        fileId = components.getOrDefault(EmuComponents.LINK_ID, null);
        cartridge = components.getOrDefault(EmuComponents.CARTRIDGE, null);
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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        if(fileId != null)
            nbt.putUuid("file_id", fileId);
        if(cartridge != null)
            nbt.put("cartridge", cartridge.toNbt(registries));
        super.writeNbt(nbt, registries);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        if(nbt.contains("file_id"))
            fileId = nbt.getUuid("file_id");
        if(nbt.contains("cartridge"))
            cartridge = ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("cartridge"));
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
