package com.districtlife.phone.dynmap;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stockage cote client de la liste des points de carte.
 * Mis a jour par le packet PacketSyncMapPoints.
 */
@OnlyIn(Dist.CLIENT)
public class MapPointsClient {

    private static final List<MapPoint> points = new ArrayList<>();

    public static void setPoints(List<MapPoint> list) {
        points.clear();
        points.addAll(list);
    }

    public static List<MapPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }
}
