package com.limo.emumod.console;

import com.limo.emumod.components.ConsoleComponent;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GenericConsoleBlockEntity extends BlockEntity {
    public UUID fileId;
    public ConsoleComponent consoleId;
    public ItemContainerContents cartridge;

    public GenericConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(EmuBlockEntities.GENERIC_CONSOLE, pos, state);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        fileId = components.getOrDefault(EmuComponents.CONSOLE_LINK_ID, null);
        consoleId = components.getOrDefault(EmuComponents.CONSOLE, new ConsoleComponent(UUID.randomUUID()));
        cartridge = components.getOrDefault(EmuComponents.CARTRIDGE, ItemContainerContents.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if(fileId != null)
            builder.set(EmuComponents.CONSOLE_LINK_ID, fileId);
        if(consoleId != null)
            builder.set(EmuComponents.CONSOLE, consoleId);
        if(cartridge != null)
            builder.set(EmuComponents.CARTRIDGE, cartridge);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);

        if(fileId != null)
            view.store("file_id", UUIDUtil.AUTHLIB_CODEC, fileId);
        view.store("console_id", ConsoleComponent.CODEC, consoleId);
        view.store("cartridge", ItemContainerContents.CODEC, cartridge);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);

        fileId = view.read("file_id", UUIDUtil.AUTHLIB_CODEC).orElse(null);
        consoleId = view.read("console_id", ConsoleComponent.CODEC).orElse(new ConsoleComponent(UUID.randomUUID()));
        cartridge = view.read("cartridge", ItemContainerContents.CODEC).orElse(ItemContainerContents.EMPTY);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
