package com.districtlife.phone.events;

import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketSyncNews;
import com.districtlife.phone.news.NewsArticle;
import com.districtlife.phone.news.NewsManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Evenements communs (Forge event bus, cote serveur).
 */
public class CommonEvents {

    /**
     * A la connexion d'un joueur : envoie tous les articles de news existants.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

        List<NewsArticle> all = NewsManager.getAll();
        if (!all.isEmpty()) {
            PacketHandler.sendToPlayer(new PacketSyncNews(new ArrayList<>(all)), player);
        }
    }
}
