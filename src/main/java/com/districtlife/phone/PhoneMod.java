package com.districtlife.phone;

import com.districtlife.phone.command.PhoneCommand;
import com.districtlife.phone.events.CommonEvents;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PhoneClientHandler;
import com.districtlife.phone.news.NewsManager;
import com.districtlife.phone.registry.ModBlocks;
import com.districtlife.phone.registry.ModItems;
import com.districtlife.phone.registry.ModSounds;
import com.districtlife.phone.registry.ModTileEntities;
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

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModTileEntities.TILE_ENTITIES.register(modBus);
        PacketHandler.register();

        MinecraftForge.EVENT_BUS.addListener(PhoneCommand::onRegisterCommands);
        MinecraftForge.EVENT_BUS.register(CommonEvents.class);
        MinecraftForge.EVENT_BUS.addListener(NewsManager::onServerStarting);

        // HUD overlay + gestionnaire paquets reseau (client uniquement)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MinecraftForge.EVENT_BUS.register(PhoneCallHud.class));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MinecraftForge.EVENT_BUS.register(PhoneClientHandler.class));
    }
}
