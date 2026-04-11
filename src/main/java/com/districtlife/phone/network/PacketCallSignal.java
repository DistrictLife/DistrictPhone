package com.districtlife.phone.network;

import com.districtlife.phone.call.CallManager;
import com.districtlife.phone.call.CallSignal;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye CLIENT -> SERVER : signaux d'appel (initier/accepter/refuser/raccrocher). */
public class PacketCallSignal {

    private final CallSignal signal;
    private final String     myPhone;
    private final String     targetPhone;

    public PacketCallSignal(CallSignal signal, String myPhone, String targetPhone) {
        this.signal      = signal;
        this.myPhone     = myPhone;
        this.targetPhone = targetPhone;
    }

    public static void encode(PacketCallSignal p, PacketBuffer buf) {
        buf.writeByte(p.signal.getId());
        buf.writeUtf(p.myPhone, 20);
        buf.writeUtf(p.targetPhone, 20);
    }

    public static PacketCallSignal decode(PacketBuffer buf) {
        return new PacketCallSignal(
                CallSignal.fromId(buf.readByte()),
                buf.readUtf(20),
                buf.readUtf(20));
    }

    public static void handle(PacketCallSignal packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            // Verifie que le joueur possede bien ce telephone
            ItemStack stack = PhoneItem.findPhoneStack(player, packet.myPhone);
            if (stack.isEmpty()) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            switch (packet.signal) {
                case CALL:
                    CallManager.initiateCall(packet.myPhone, packet.targetPhone, player, server);
                    break;
                case ACCEPT:
                    CallManager.acceptCall(packet.myPhone, packet.targetPhone, player, server);
                    break;
                case DECLINE:
                    CallManager.declineCall(packet.myPhone, packet.targetPhone, server);
                    break;
                case HANGUP:
                    CallManager.hangup(packet.myPhone, server);
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
