package com.districtlife.phone.registry;

import com.districtlife.phone.block.PhoneFixTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModTileEntities {

    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, "districtlife_phone");

    public static final RegistryObject<TileEntityType<PhoneFixTileEntity>> PHONE_FIX =
            TILE_ENTITIES.register("phone_fix", () ->
                    TileEntityType.Builder
                            .of(PhoneFixTileEntity::new, ModBlocks.PHONE_FIX.get())
                            .build(null));
}
