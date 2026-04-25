package com.districtlife.phone.registry;

import com.districtlife.phone.PhoneMod;
import com.districtlife.phone.block.PhoneFixBlock;
import com.districtlife.phone.item.PhoneFixItem;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, PhoneMod.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, PhoneMod.MOD_ID);

    public static final RegistryObject<Block> PHONE_FIX = BLOCKS.register("phone_fix",
            PhoneFixBlock::new);

    public static final RegistryObject<Item> PHONE_FIX_ITEM = ITEMS.register("phone_fix",
            () -> new PhoneFixItem(PHONE_FIX.get(), new Item.Properties().tab(ItemGroup.TAB_MISC)));
}
