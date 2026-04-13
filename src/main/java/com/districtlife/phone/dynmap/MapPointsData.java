package com.districtlife.phone.dynmap;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistance serveur des points de carte crees par les admins.
 */
public class MapPointsData extends WorldSavedData {

    private static final String KEY = "districtlife_map_points";

    private final List<MapPoint> points = new ArrayList<>();

    public MapPointsData() {
        super(KEY);
    }

    public static MapPointsData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(MapPointsData::new, KEY);
    }

    public List<MapPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public void addPoint(MapPoint point) {
        points.add(point);
        setDirty();
    }

    @Override
    public void load(CompoundNBT nbt) {
        points.clear();
        ListNBT list = nbt.getList("points", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT tag = list.getCompound(i);
            points.add(new MapPoint(
                    tag.getString("name"),
                    tag.getInt("color"),
                    tag.getInt("x"),
                    tag.getInt("z")
            ));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        ListNBT list = new ListNBT();
        for (MapPoint p : points) {
            CompoundNBT tag = new CompoundNBT();
            tag.putString("name",  p.name);
            tag.putInt("color",    p.color);
            tag.putInt("x",        p.x);
            tag.putInt("z",        p.z);
            list.add(tag);
        }
        nbt.put("points", list);
        return nbt;
    }
}
