package com.districtlife.phone.news;

import com.google.gson.*;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Stockage serveur-side des articles de news.
 * Les articles sont persistes dans config/districtlife_phone/news.json.
 * Liste triee du plus recent au plus ancien (index 0 = plus recent).
 */
public final class NewsManager {

    private static final int    MAX_ARTICLES = 50;
    private static final Gson   GSON         = new GsonBuilder().setPrettyPrinting().create();

    private static final List<NewsArticle> articles = new ArrayList<>();
    private static int nextId = 1;

    private NewsManager() {}

    // -------------------------------------------------------------------------
    // Cycle de vie serveur
    // -------------------------------------------------------------------------

    public static void onServerStarting(FMLServerStartingEvent event) {
        load();
    }

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    /**
     * Cree et enregistre un nouvel article.
     * Retourne l'article cree (avec son id definitif).
     */
    public static NewsArticle publish(String title, String author, String content, long tickRP) {
        title   = title.length()   > 500  ? title.substring(0, 500)   : title;
        content = content.length() > 2000 ? content.substring(0, 2000) : content;

        NewsArticle article = new NewsArticle(nextId++, title, author, content, tickRP);
        articles.add(0, article);   // plus recent en tete

        // Limite a MAX_ARTICLES
        while (articles.size() > MAX_ARTICLES) {
            articles.remove(articles.size() - 1);
        }

        save();
        return article;
    }

    /** Retourne une vue non-modifiable de la liste (plus recent en premier). */
    public static List<NewsArticle> getAll() {
        return Collections.unmodifiableList(articles);
    }

    // -------------------------------------------------------------------------
    // Persistance JSON
    // -------------------------------------------------------------------------

    private static void load() {
        articles.clear();
        nextId = 1;

        Path path = getFilePath();
        if (!Files.exists(path)) return;

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonArray array = GSON.fromJson(reader, JsonArray.class);
            if (array == null) return;

            for (JsonElement elem : array) {
                JsonObject obj = elem.getAsJsonObject();
                int    id      = obj.get("id").getAsInt();
                String title   = obj.get("title").getAsString();
                String author  = obj.get("author").getAsString();
                String content = obj.get("content").getAsString();
                long   tickRP  = obj.get("tickRP").getAsLong();

                articles.add(new NewsArticle(id, title, author, content, tickRP));
                if (id >= nextId) nextId = id + 1;
            }
        } catch (Exception e) {
            System.err.println("[DistrictLife Phone] Erreur lecture news.json : " + e.getMessage());
        }
    }

    private static void save() {
        Path path = getFilePath();
        try {
            Files.createDirectories(path.getParent());
            JsonArray array = new JsonArray();
            for (NewsArticle a : articles) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id",      a.id);
                obj.addProperty("title",   a.title);
                obj.addProperty("author",  a.author);
                obj.addProperty("content", a.content);
                obj.addProperty("tickRP",  a.tickRP);
                array.add(obj);
            }
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(array, writer);
            }
        } catch (Exception e) {
            System.err.println("[DistrictLife Phone] Erreur sauvegarde news.json : " + e.getMessage());
        }
    }

    private static Path getFilePath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve("districtlife_phone")
                .resolve("news.json");
    }
}
