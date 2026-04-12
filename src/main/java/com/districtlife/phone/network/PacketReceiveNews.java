package com.districtlife.phone.network;

import com.districtlife.phone.news.NewsArticle;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye SERVER -> CLIENT : un nouvel article publie (broadcast a tous les connectes). */
public class PacketReceiveNews {

    final NewsArticle article;

    public PacketReceiveNews(NewsArticle article) {
        this.article = article;
    }

    public static void encode(PacketReceiveNews p, PacketBuffer buf) {
        NewsArticle.encode(p.article, buf);
    }

    public static PacketReceiveNews decode(PacketBuffer buf) {
        return new PacketReceiveNews(NewsArticle.decode(buf));
    }

    public static void handle(PacketReceiveNews packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                MinecraftForge.EVENT_BUS.post(new PhoneNetEvent.ReceiveNews(packet.article))
        );
        ctx.get().setPacketHandled(true);
    }
}
