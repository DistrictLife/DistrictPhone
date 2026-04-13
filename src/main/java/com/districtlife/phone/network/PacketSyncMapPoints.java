package com.districtlife.phone.network;

import com.districtlife.phone.dynmap.MapPoint;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * SERVER -> CLIENT : synchronise la liste complete des points de carte.
 * Envoye a la connexion et apres chaque /phone map-point.
 */
public class PacketSyncMapPoints {

    private final List<MapPoint> points;

    public PacketSyncMapPoints(List<MapPoint> points) {
        this.points = new ArrayList<>(points);
    }

    public static void encode(PacketSyncMapPoints pkt, PacketBuffer buf) {
        buf.writeInt(pkt.points.size());
        for (MapPoint p : pkt.points) {
            buf.writeUtf(p.name, 256);
            buf.writeInt(p.color);
            buf.writeInt(p.x);
            buf.writeInt(p.z);
        }
    }

    public static PacketSyncMapPoints decode(PacketBuffer buf) {
        int count = buf.readInt();
        List<MapPoint> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name  = buf.readUtf(256);
            int    color = buf.readInt();
            int    x     = buf.readInt();
            int    z     = buf.readInt();
            list.add(new MapPoint(name, color, x, z));
        }
        return new PacketSyncMapPoints(list);
    }

    public static void handle(PacketSyncMapPoints pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.districtlife.phone.dynmap.MapPointsClient.setPoints(pkt.points))
        );
        ctx.get().setPacketHandled(true);
    }
}
