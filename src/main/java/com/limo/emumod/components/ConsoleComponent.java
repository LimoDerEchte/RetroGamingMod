package com.limo.emumod.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Uuids;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import static com.limo.emumod.registry.EmuComponents.CONSOLE;

public record ConsoleComponent(UUID consoleId) implements TooltipAppender {

    public static final Codec<ConsoleComponent> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Uuids.CODEC.optionalFieldOf("consoleId", UUID.randomUUID()).forGetter(c -> c.consoleId)
            ).apply(inst, ConsoleComponent::new));

    public static final PacketCodec<RegistryByteBuf, ConsoleComponent> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, (comp) -> comp.consoleId,
            ConsoleComponent::new);

    @Override
    public void appendTooltip(Item.TooltipContext ctx, Consumer<Text> textConsumer, TooltipType type, ComponentsAccess components) {
        textConsumer.accept(Text.translatable("item.emumod.tooltip.console",
                Objects.requireNonNull(components.get(CONSOLE)).consoleId).formatted(Formatting.GRAY));
    }
}
