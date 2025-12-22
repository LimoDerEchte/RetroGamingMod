package com.limo.emumod.components;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Uuids;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import static com.limo.emumod.registry.EmuComponents.GAME;

public record GameComponent(UUID fileId, String gameTitle) implements TooltipAppender {

    public static final Codec<GameComponent> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Uuids.CODEC.fieldOf("fileId").forGetter(c -> c.fileId),
                    Codec.STRING.fieldOf("gameTitle").forGetter(c -> c.gameTitle)
            ).apply(inst, GameComponent::new));

    public static final PacketCodec<RegistryByteBuf, GameComponent> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, (comp) -> comp.fileId,
            PacketCodecs.STRING, (comp) -> comp.gameTitle,
            GameComponent::new);

    @Override
    public void appendTooltip(Item.TooltipContext ctx, Consumer<Text> textConsumer, TooltipType type, ComponentsAccess components) {
        textConsumer.accept(Text.translatable("item.emumod.tooltip.game",
                Objects.requireNonNull(components.get(GAME)).gameTitle).formatted(Formatting.GRAY));
        textConsumer.accept(Text.translatable("item.emumod.tooltip.file",
                Objects.requireNonNull(components.get(GAME)).fileId).formatted(Formatting.GRAY));
    }
}
