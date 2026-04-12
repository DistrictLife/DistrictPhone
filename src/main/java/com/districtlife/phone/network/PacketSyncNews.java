package com.districtlife.phone.network;

import com.districtlife.phone.news.NewsArticle;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Envoye SERVER -> CLIENT a la connexion : tous les articles existants. */
public class PacketSyncNews {

    final List<NewsArticle> articles;

    public PacketSyncNews(List<NewsArticle> articles) {
        this.articles = articles;
    }

    public static void encode(PacketSyncNews p, PacketBuffer buf) {
        buf.writeInt(p.articles.size());
        for (NewsArticle a : p.articles) {
            NewsArticle.encode(a, buf);
        }
    }

    public static PacketSyncNews decode(PacketBuffer buf) {
        int count = buf.readInt();
        List<NewsArticle> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(NewsArticle.decode(buf));
        }
        return new PacketSyncNews(list);
    }

    public static void handle(PacketSyncNews packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                MinecraftForge.EVENT_BUS.post(new PhoneNetEvent.SyncNews(packet.articles))
        );
        ctx.get().setPacketHandled(true);
    }
}
