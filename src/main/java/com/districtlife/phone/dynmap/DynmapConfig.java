package com.districtlife.phone.dynmap;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Persistance serveur de l'URL de base de Dynmap.
 * Stockee dans le WorldSavedData de l'Overworld.
 *
 * L'admin fournit juste l'URL de base (ex : http://host:8123).
 * La decouverte du chemin exact des tuiles est faite cote client.
 */
public class DynmapConfig extends WorldSavedData {

    private static final String KEY = "districtlife_dynmap";

    private String baseUrl = "";

    public DynmapConfig() {
        super(KEY);
    }

    public static DynmapConfig get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(DynmapConfig::new, KEY);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String url) {
        this.baseUrl = url == null ? "" : url.trim();
        setDirty();
    }

    @Override
    public void load(CompoundNBT nbt) {
        baseUrl = nbt.getString("baseUrl");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.putString("baseUrl", baseUrl);
        return nbt;
    }
}
