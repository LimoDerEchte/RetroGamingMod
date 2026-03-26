package com.limo.emumod.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import static com.limo.emumod.registry.EmuComponents.CONSOLE;

public record ConsoleComponent(UUID consoleId) implements TooltipProvider {

    public static final Codec<ConsoleComponent> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    UUIDUtil.AUTHLIB_CODEC.optionalFieldOf("consoleId", UUID.randomUUID()).forGetter(c -> c.consoleId)
            ).apply(inst, ConsoleComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConsoleComponent> PACKET_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, (comp) -> comp.consoleId,
            ConsoleComponent::new);

    @Override
    public void addToTooltip(Item.TooltipContext ctx, Consumer<Component> textConsumer, TooltipFlag type, DataComponentGetter components) {
        textConsumer.accept(Component.translatable("item.emumod.tooltip.console",
                Objects.requireNonNull(components.get(CONSOLE)).consoleId).withStyle(ChatFormatting.GRAY));
    }
}
