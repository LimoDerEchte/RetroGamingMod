package com.limo.emumod.registry;

import com.limo.emumod.cartridge.CartridgeItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;

public class EmuItems {
    public static final Item CARTRIDGE = register(new CartridgeItem(), ItemId.Registry.CARTRIDGE);
    public static final Item BROKEN_CARTRIDGE = register(new Item(new Item.Settings().maxCount(8)
            .registryKey(ItemId.Registry.BROKEN_CARTRIDGE)), ItemId.Registry.BROKEN_CARTRIDGE);

    public static final ItemGroup MAIN_GROUP = register(FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.emumod.main"))
            .icon(CARTRIDGE::getDefaultStack)
            .build(), ItemId.Registry.MAIN_GROUP);

    public static Item register(Item item, RegistryKey<Item> registryKey) {
        return Registry.register(Registries.ITEM, registryKey.getValue(), item);
    }

    public static ItemGroup register(ItemGroup group, RegistryKey<ItemGroup> registryKey) {
        return Registry.register(Registries.ITEM_GROUP, registryKey.getValue(), group);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemId.Registry.MAIN_GROUP).register(group -> {
            group.add(CARTRIDGE);
            group.add(BROKEN_CARTRIDGE);
        });
    }
}
