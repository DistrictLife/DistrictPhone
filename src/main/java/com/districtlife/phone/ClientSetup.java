package com.districtlife.phone;

import com.districtlife.phone.registry.ModItems;
import com.districtlife.phone.screen.PhoneScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
public class ClientSetup {

    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
            ItemModelsProperties.register(
                ModItems.PHONE.get(),
                new ResourceLocation(PhoneMod.MOD_ID, "screen_open"),
                (stack, world, entity) ->
                    Minecraft.getInstance().screen instanceof PhoneScreen ? 1.0f : 0.0f
            )
        );
    }
}
