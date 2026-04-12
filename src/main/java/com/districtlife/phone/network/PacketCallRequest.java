package com.districtlife.phone.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye SERVER -> CLIENT : notifie d'un appel entrant. */
public class PacketCallRequest {

    final String callerPhone;
    final String callerMcName;

    public PacketCallRequest(String callerPhone, String callerMcName) {
        this.callerPhone  = callerPhone;
        this.callerMcName = callerMcName;
    }

    public static void encode(PacketCallRequest p, PacketBuffer buf) {
        buf.writeUtf(p.callerPhone, 20);
        buf.writeUtf(p.callerMcName, 64);
    }

    public static PacketCallRequest decode(PacketBuffer buf) {
        return new PacketCallRequest(buf.readUtf(20), buf.readUtf(64));
    }

    public static void handle(PacketCallRequest packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            MinecraftForge.EVENT_BUS.post(
                new PhoneNetEvent.CallRequest(packet.callerPhone, packet.callerMcName))
        );
        ctx.get().setPacketHandled(true);
    }
}
