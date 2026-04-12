package com.districtlife.phone.news;

import net.minecraft.network.PacketBuffer;

/** Modele de donnees d'un article de l'app News. Immuable, partageable S/C. */
public class NewsArticle {

    public final int    id;
    public final String title;
    public final String author;
    public final String content;
    /** Tick monde absolu au moment de la publication (pour calculer la date/heure RP). */
    public final long   tickRP;

    public NewsArticle(int id, String title, String author, String content, long tickRP) {
        this.id      = id;
        this.title   = title;
        this.author  = author;
        this.content = content;
        this.tickRP  = tickRP;
    }

    // -------------------------------------------------------------------------
    // Serialisation PacketBuffer (S <-> C)
    // -------------------------------------------------------------------------

    public static void encode(NewsArticle a, PacketBuffer buf) {
        buf.writeInt(a.id);
        buf.writeUtf(a.title,   500);
        buf.writeUtf(a.author,  64);
        buf.writeUtf(a.content, 2000);
        buf.writeLong(a.tickRP);
    }

    public static NewsArticle decode(PacketBuffer buf) {
        return new NewsArticle(
                buf.readInt(),
                buf.readUtf(500),
                buf.readUtf(64),
                buf.readUtf(2000),
                buf.readLong());
    }
}
