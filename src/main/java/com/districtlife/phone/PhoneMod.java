package com.districtlife.phone;

import com.districtlife.phone.command.PhoneCommand;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.registry.ModItems;
import com.districtlife.phone.screen.hud.PhoneCallHud;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;

@Mod(PhoneMod.MOD_ID)
public class PhoneMod {

    public static final String MOD_ID = "districtlife_phone";

    public PhoneMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        PacketHandler.register();

        MinecraftForge.EVENT_BUS.addListener(PhoneCommand::onRegisterCommands);

        // HUD overlay (client uniquement)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MinecraftForge.EVENT_BUS.register(PhoneCallHud.class));
    }
}
