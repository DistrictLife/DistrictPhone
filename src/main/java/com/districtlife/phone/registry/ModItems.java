package com.districtlife.phone.registry;

import com.districtlife.phone.PhoneMod;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PhoneMod.MOD_ID);

    public static final RegistryObject<Item> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties().stacksTo(1).tab(ItemGroup.TAB_MISC)));
}
