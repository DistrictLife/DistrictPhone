package com.districtlife.phone.dynmap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cote client : decouverte automatique et cache des tuiles Dynmap.
 *
 * Les tuiles sont stockees comme GL texture IDs pour eviter les problemes
 * de nommage avec TextureManager.register(String, ...).
 */
@OnlyIn(Dist.CLIENT)
public class DynmapClient {

    private static volatile String  baseUrl        = "";
    private static volatile String  tilePrefix     = "";
    private static volatile int     tileBlocksBase = 32;
    private static volatile int     tilePixelSize  = 128;
    private static volatile String  imageFormat    = "png";
    private static volatile int     maxDynzoom     = 5;
    private static volatile boolean discovering    = false;

    /** Sentinel : tuile en echec definitif. */
    private static final int FAILED_ID = -1;

    /**
     * Cache GL texture ID par cle "dynzoom_tx_tz".
     * FAILED_ID (-1) = echec definitif.
     * Absent = pas encore charge.
     */
    private static final ConcurrentHashMap<String, Integer>         tileCache  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap.KeySetView<String,Boolean> loading  = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, Integer>          failCount = new ConcurrentHashMap<>();

    /**
     * Garde les references DynamicTexture pour empecher le GC de supprimer
     * les textures GL (le finalizer de DynamicTexture appelle releaseId()).
     */
    private static final List<DynamicTexture> tileTextures = new ArrayList<>();

    /** File de resultats termines, drainee sur le main thread. */
    private static final ConcurrentLinkedQueue<Object[]> pendingTextures = new ConcurrentLinkedQueue<>();

    private static final int MAX_RETRIES = 3;

    private static final ExecutorService executor = Executors.newFixedThreadPool(4,
            r -> { Thread t = new Thread(r, "DynmapTileLoader"); t.setDaemon(true); return t; });

    /** Dernier code d'erreur observe (debug). */
    private static volatile String lastError = "none";

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    public static void setBaseUrl(String url) {
        String clean = url == null ? "" : url.trim();
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);

        if (baseUrl.equals(clean)) return;

        clearCache();
        baseUrl    = clean;
        tilePrefix = "";

        if (!clean.isEmpty()) {
            discovering = true;
            final String cleanUrl = clean;
            executor.submit(() -> discoverConfig(cleanUrl));
        }
    }

    public static boolean hasUrl()  { return !baseUrl.isEmpty(); }
    public static boolean isReady() { return !tilePrefix.isEmpty(); }

    public static String getDebugInfo() {
        return "url=" + !baseUrl.isEmpty()
             + " rdy=" + !tilePrefix.isEmpty()
             + " disc=" + discovering
             + " cache=" + tileCache.size()
             + " load=" + loading.size()
             + " fail=" + failCount.size()
             + " pend=" + pendingTextures.size()
             + " err=" + lastError;
    }

    public static int getTileBlocksBase() { return tileBlocksBase; }
    public static int getTilePixelSize()  { return tilePixelSize; }
    public static int getMaxDynzoom()     { return maxDynzoom; }

    // -------------------------------------------------------------------------
    // Acces aux tuiles  (appele depuis le main thread / render)
    // -------------------------------------------------------------------------

    /**
     * Draine la file pendingTextures et enregistre les textures sur le main thread.
     * DOIT etre appele depuis AppMap.render() (contexte GL requis).
     */
    public static void processPendingTextures() {
        Object[] entry;
        while ((entry = pendingTextures.poll()) != null) {
            String      key  = (String)      entry[0];
            NativeImage img  = (NativeImage) entry[1];

            if (img == null) {
                // Echec du telechargement ou du decodage
                int count = failCount.getOrDefault(key, 0) + 1;
                failCount.put(key, count);
                if (count >= MAX_RETRIES) {
                    tileCache.put(key, FAILED_ID);
                }
                loading.remove(key);
            } else {
                try {
                    // Cree la DynamicTexture sur le main thread (contexte GL)
                    DynamicTexture tex = new DynamicTexture(img);
                    int glId = tex.getId();
                    if (glId > 0) {
                        tileTextures.add(tex);      // garder la reference vivante
                        tileCache.put(key, glId);
                    } else {
                        lastError = "gl:id=" + glId;
                        int count = failCount.getOrDefault(key, 0) + 1;
                        failCount.put(key, count);
                        if (count >= MAX_RETRIES) tileCache.put(key, FAILED_ID);
                        tex.close();
                    }
                } catch (Exception e) {
                    lastError = "tex:" + e.getClass().getSimpleName() + ":" + e.getMessage();
                    int count = failCount.getOrDefault(key, 0) + 1;
                    failCount.put(key, count);
                    if (count >= MAX_RETRIES) tileCache.put(key, FAILED_ID);
                } finally {
                    loading.remove(key);
                }
            }
        }
    }

    /**
     * Lie la texture GL de la tuile si disponible.
     * @return true si la tuile est prete et liee, false sinon (telechargement lance).
     */
    public static boolean bindTile(int tx, int tz, int dynzoom) {
        if (tilePrefix.isEmpty()) return false;

        String key = dynzoom + "_" + tx + "_" + tz;

        Integer cached = tileCache.get(key);
        if (cached != null) {
            if (cached == FAILED_ID) return false;
            RenderSystem.bindTexture(cached);
            return true;
        }

        if (loading.add(key)) {
            final String url = buildTileUrl(tx, tz, dynzoom);
            executor.submit(() -> downloadTile(key, url));
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Selection du zoom optimal
    // -------------------------------------------------------------------------

    /** Toujours utiliser la qualite maximale (dynzoom=0). */
    public static int selectDynzoom(float screenZoom) {
        return 0;
    }

    // -------------------------------------------------------------------------
    // Decouverte de la configuration
    // -------------------------------------------------------------------------

    private static void discoverConfig(String base) {
        try {
            String json = fetchText(base + "/up/configuration", 5000);
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray worlds = root.getAsJsonArray("worlds");

            if (worlds != null && worlds.size() > 0) {
                JsonObject world = worlds.get(0).getAsJsonObject();
                String worldName = world.get("name").getAsString();

                String mapPrefix  = "flat";
                int    scale      = 4;
                String imgFmt     = "jpg";
                int    tilePixels = 128;
                int    zoomOut    = 5;

                JsonArray maps = world.getAsJsonArray("maps");
                if (maps != null && maps.size() > 0) {
                    JsonObject chosen = maps.get(0).getAsJsonObject();
                    for (int i = 0; i < maps.size(); i++) {
                        JsonObject m = maps.get(i).getAsJsonObject();
                        if (m.has("prefix") && "flat".equals(m.get("prefix").getAsString())) {
                            chosen = m;
                            break;
                        }
                    }
                    if (chosen.has("prefix"))      mapPrefix  = chosen.get("prefix").getAsString();
                    if (chosen.has("scale"))        scale      = chosen.get("scale").getAsInt();
                    if (chosen.has("image-format")) imgFmt     = chosen.get("image-format").getAsString();
                    if (chosen.has("tilescale")) {
                        int ts = chosen.get("tilescale").getAsInt();
                        tilePixels = 128 * (1 << ts);
                    }
                    if (chosen.has("mapzoomout"))   zoomOut    = chosen.get("mapzoomout").getAsInt();
                }

                tileBlocksBase = tilePixels / Math.max(1, scale);
                tilePixelSize  = tilePixels;
                imageFormat    = imgFmt;
                maxDynzoom     = zoomOut;
                tilePrefix     = base + "/tiles/" + worldName + "/" + mapPrefix;
            } else {
                applyFallback(base);
            }
        } catch (Exception e) {
            lastError = "disc:" + e.getMessage();
            applyFallback(base);
        } finally {
            discovering = false;
        }
    }

    private static void applyFallback(String base) {
        tileBlocksBase = 32;
        tilePixelSize  = 128;
        imageFormat    = "jpg";
        maxDynzoom     = 5;
        tilePrefix     = base + "/tiles/world/flat";
    }

    // -------------------------------------------------------------------------
    // Telechargement
    // -------------------------------------------------------------------------

    private static String buildTileUrl(int tx, int tz, int dynzoom) {
        String fmt = "." + imageFormat;
        if (dynzoom == 0) {
            return tilePrefix + "/" + tx + "_" + tz + fmt;
        } else {
            return tilePrefix + "/z" + dynzoom + "/" + tx + "_" + tz + fmt;
        }
    }

    private static void downloadTile(String key, String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                lastError = "http" + code + ":" + urlStr.substring(urlStr.lastIndexOf('/') + 1);
                conn.disconnect();
                pendingTextures.add(new Object[]{ key, null });
                return;
            }

            byte[] data;
            try (InputStream is = conn.getInputStream()) {
                data = readAllBytes(is);
            }
            conn.disconnect();

            NativeImage img = decodeViaImageIO(data);
            if (img == null) img = decodeViaNativeImage(data);

            pendingTextures.add(new Object[]{ key, img }); // img==null => echec
        } catch (Exception e) {
            lastError = "ex:" + e.getClass().getSimpleName();
            pendingTextures.add(new Object[]{ key, null });
        }
    }

    private static NativeImage decodeViaImageIO(byte[] data) {
        try {
            java.awt.image.BufferedImage bi =
                    javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (bi == null) return null;

            int w = bi.getWidth();
            int h = bi.getHeight();
            NativeImage img = new NativeImage(w, h, false);
            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {
                    int argb = bi.getRGB(px, py);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >>  8) & 0xFF;
                    int b =  argb        & 0xFF;
                    img.setPixelRGBA(px, py, r | (g << 8) | (b << 16) | (a << 24));
                }
            }
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    private static NativeImage decodeViaNativeImage(byte[] data) {
        try {
            return NativeImage.read(new java.io.ByteArrayInputStream(data));
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readAllBytes(InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    private static String fetchText(String urlStr, int timeoutMs) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestProperty("User-Agent", "DistrictLife-Phone/1.0");
        conn.connect();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new RuntimeException("HTTP " + conn.getResponseCode());
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static void clearCache() {
        // Libere les textures GL
        for (DynamicTexture tex : tileTextures) {
            try { tex.close(); } catch (Exception ignored) {}
        }
        tileTextures.clear();
        tileCache.clear();
        loading.clear();
        failCount.clear();
        pendingTextures.clear();
    }
}
