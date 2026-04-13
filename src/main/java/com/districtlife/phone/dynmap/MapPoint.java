package com.districtlife.phone.dynmap;

/**
 * Point d'interet sur la carte (cree par un admin via /phone map-point).
 *
 * x/z sont les coordonnees bloc Minecraft.
 * color est au format ARGB (alpha toujours 0xFF).
 */
public class MapPoint {

    public final String name;
    public final int    color; // ARGB
    public final int    x;
    public final int    z;

    public MapPoint(String name, int color, int x, int z) {
        this.name  = name;
        this.color = color;
        this.x     = x;
        this.z     = z;
    }
}
