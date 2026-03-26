package com.limo.emumod.components;

import com.mojang.datafixers.util.Pair;
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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import static com.limo.emumod.registry.EmuComponents.GAME;

public record GameComponent(UUID fileId, String gameTitle) implements TooltipProvider {

    public static final Codec<GameComponent> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    UUIDUtil.AUTHLIB_CODEC.fieldOf("uuid").forGetter(c -> c.fileId),
                    Codec.STRING.fieldOf("gameTitle").forGetter(c -> c.gameTitle)
            ).apply(inst, GameComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GameComponent> PACKET_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, (comp) -> comp.fileId,
            ByteBufCodecs.STRING_UTF8, (comp) -> comp.gameTitle,
            GameComponent::new);

    @Override
    public void addToTooltip(Item.TooltipContext ctx, Consumer<Component> textConsumer, TooltipFlag type, DataComponentGetter components) {
        textConsumer.accept(Component.translatable("item.emumod.tooltip.game",
                Objects.requireNonNull(components.get(GAME)).gameTitle).withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable("item.emumod.tooltip.file",
                Objects.requireNonNull(components.get(GAME)).fileId).withStyle(ChatFormatting.GRAY));
    }
}
