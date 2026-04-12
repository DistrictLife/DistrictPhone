package com.districtlife.phone.news;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

/**
 * Cache client-side des articles de news.
 * Alimente par PacketSyncNews (connexion) et PacketReceiveNews (publication en direct).
 * Gere l'etat "non lu" : un article est non lu si son id est superieur au dernier id vu.
 */
@OnlyIn(Dist.CLIENT)
public final class NewsClientCache {

    private static final List<NewsArticle> articles   = new ArrayList<>();
    /** Id du dernier article affiche par le joueur. -1 = jamais ouvert. */
    private static int lastSeenId = -1;

    private NewsClientCache() {}

    // -------------------------------------------------------------------------
    // Mise a jour depuis les paquets
    // -------------------------------------------------------------------------

    /** Remplace toute la liste (envoi initial a la connexion). */
    public static void setAll(List<NewsArticle> incoming) {
        articles.clear();
        articles.addAll(incoming);
        sortDescending();
    }

    /** Ajoute ou met a jour un article (publication en direct). */
    public static void addOrUpdate(NewsArticle article) {
        articles.removeIf(a -> a.id == article.id);
        articles.add(0, article);
        sortDescending();
    }

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    public static List<NewsArticle> getAll() {
        return Collections.unmodifiableList(articles);
    }

    /** Vrai si au moins un article n'a pas encore ete consulte. */
    public static boolean hasUnread() {
        if (articles.isEmpty()) return false;
        return articles.get(0).id > lastSeenId;
    }

    /** Appele quand le joueur ouvre l'app News : marque tous comme lus. */
    public static void markAllRead() {
        if (!articles.isEmpty()) {
            lastSeenId = articles.get(0).id;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void sortDescending() {
        articles.sort((a, b) -> Integer.compare(b.id, a.id));
    }
}
