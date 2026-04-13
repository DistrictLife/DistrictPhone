package com.districtlife.phone.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * SERVER -> CLIENT : synchronise l'URL de base Dynmap.
 * Envoye a la connexion du joueur et apres chaque /phone map <url>.
 */
public class PacketSyncDynmap {

    private final String baseUrl;

    public PacketSyncDynmap(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static void encode(PacketSyncDynmap pkt, PacketBuffer buf) {
        buf.writeUtf(pkt.baseUrl, 512);
    }

    public static PacketSyncDynmap decode(PacketBuffer buf) {
        return new PacketSyncDynmap(buf.readUtf(512));
    }

    public static void handle(PacketSyncDynmap pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.districtlife.phone.dynmap.DynmapClient.setBaseUrl(pkt.baseUrl))
        );
        ctx.get().setPacketHandled(true);
    }
}
