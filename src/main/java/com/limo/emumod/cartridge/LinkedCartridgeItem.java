package com.limo.emumod.cartridge;

import com.limo.emumod.components.ConsoleComponent;
import com.limo.emumod.components.GameComponent;
import com.limo.emumod.console.GenericHandheldItem;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.function.TriFunction;

import java.io.File;
import java.util.UUID;

import static com.limo.emumod.registry.EmuComponents.CONSOLE;
import static com.limo.emumod.registry.EmuComponents.GAME;

public class LinkedCartridgeItem extends Item {
    public Item linkItem;
    private final boolean isHandheld;
    private String fileType;
    private Runnable clearLinkItem;
    private TriFunction<Player, UUID, UUID, Boolean> start;

    public LinkedCartridgeItem(ResourceKey<Item> key, String fileType, Runnable clearLinkItem, TriFunction<Player, UUID, UUID, Boolean> start) {
        super(new Properties().stacksTo(1).setId(key));
        this.isHandheld = true;
        this.fileType = fileType;
        this.clearLinkItem = clearLinkItem;
        this.start = start;
    }

    public LinkedCartridgeItem(ResourceKey<Item> key) {
        super(new Properties().stacksTo(1).setId(key));
        isHandheld = false;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if(world.isClientSide())
            return super.use(world, user, hand);
        ItemStack stack = user.getItemInHand(hand);
        if(!isHandheld) {
            return InteractionResult.PASS;
        }
        ItemStack link = GenericHandheldItem.link != null && GenericHandheldItem.link.getItem() == linkItem ?
                GenericHandheldItem.link : ItemStack.EMPTY;

        DataComponentMap components = stack.getComponents();
        if(link.getCount() > 0 && components.has(GAME)) {
            GameComponent game = components.get(GAME);
            assert game != null;
            File file = FileUtil.idToFile(game.fileId(), fileType);
            if(!file.exists()) {
                stack.setCount(0);
                user.getInventory().add(new ItemStack(EmuItems.BROKEN_CARTRIDGE));
                user.displayClientMessage(Component.translatable("item.emumod.handheld.file_deleted")
                        .withStyle(ChatFormatting.RED), true);
            } else {
                clearLinkItem.run();
                ConsoleComponent console = link.getComponents().get(CONSOLE);
                if(console == null)
                    throw new RuntimeException();
                if(start.apply(user, game.fileId(), console.consoleId())) {
                    link.applyComponents(DataComponentMap.builder().set(GAME, game).build());
                    stack.setCount(0);
                    user.displayClientMessage(Component.translatable("item.emumod.handheld.insert"), true);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
