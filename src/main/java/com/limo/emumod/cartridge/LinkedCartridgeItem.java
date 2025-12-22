package com.limo.emumod.cartridge;

import com.limo.emumod.components.GameComponent;
import com.limo.emumod.console.GenericHandheldItem;
import com.limo.emumod.registry.EmuItems;
import com.limo.emumod.util.FileUtil;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.io.File;
import java.util.UUID;
import java.util.function.BiFunction;

import static com.limo.emumod.registry.EmuComponents.GAME;

public class LinkedCartridgeItem extends Item {
    public Item linkItem;
    private final boolean isHandheld;
    private String fileType;
    private Runnable clearLinkItem;
    private BiFunction<PlayerEntity, UUID, Boolean> start;

    public LinkedCartridgeItem(RegistryKey<Item> key, String fileType, Runnable clearLinkItem, BiFunction<PlayerEntity, UUID, Boolean> start) {
        super(new Settings().maxCount(1).registryKey(key));
        this.isHandheld = true;
        this.fileType = fileType;
        this.clearLinkItem = clearLinkItem;
        this.start = start;
    }

    public LinkedCartridgeItem(RegistryKey<Item> key) {
        super(new Settings().maxCount(1).registryKey(key));
        isHandheld = false;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient())
            return super.use(world, user, hand);
        ItemStack stack = user.getStackInHand(hand);
        if(!isHandheld) {
            return ActionResult.PASS;
        }
        ItemStack link = GenericHandheldItem.link != null && GenericHandheldItem.link.getItem() == linkItem ?
                GenericHandheldItem.link : ItemStack.EMPTY;

        ComponentMap components = stack.getComponents();
        if(link.getCount() > 0 && components.contains(GAME)) {
            GameComponent game = components.get(GAME);
            assert game != null;
            File file = FileUtil.idToFile(game.fileId(), fileType);
            if(!file.exists()) {
                stack.setCount(0);
                user.getInventory().insertStack(new ItemStack(EmuItems.BROKEN_CARTRIDGE));
                user.sendMessage(Text.translatable("item.emumod.handheld.file_deleted")
                        .formatted(Formatting.RED), true);
            } else {
                clearLinkItem.run();
                if(start.apply(user, game.fileId())) {
                    link.applyComponentsFrom(ComponentMap.builder().add(GAME, game).build());
                    stack.setCount(0);
                    user.sendMessage(Text.translatable("item.emumod.handheld.insert"), true);
                }
            }
        }
        return ActionResult.PASS;
    }
}
