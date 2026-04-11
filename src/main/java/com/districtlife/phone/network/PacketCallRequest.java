package com.districtlife.phone.network;

import com.districtlife.phone.call.PhoneCallState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye SERVER -> CLIENT : notifie d'un appel entrant. */
public class PacketCallRequest {

    private final String callerPhone;
    private final String callerMcName;

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
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            // Met a jour l'etat client — AppPhone et PhoneCallHud liront cet etat
            PhoneCallState.setRinging(packet.callerPhone, packet.callerMcName);
        });
        ctx.get().setPacketHandled(true);
    }
}
