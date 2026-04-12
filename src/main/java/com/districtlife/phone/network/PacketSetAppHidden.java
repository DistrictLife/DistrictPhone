package com.districtlife.phone.network;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye CLIENT -> SERVER pour masquer ou afficher une application sur l'ecran d'accueil. */
public class PacketSetAppHidden {

    private final String phoneNumber;
    private final String appId;
    private final boolean hidden;

    public PacketSetAppHidden(String phoneNumber, String appId, boolean hidden) {
        this.phoneNumber = phoneNumber;
        this.appId       = appId;
        this.hidden      = hidden;
    }

    public static void encode(PacketSetAppHidden p, PacketBuffer buf) {
        buf.writeUtf(p.phoneNumber, 20);
        buf.writeUtf(p.appId, 64);
        buf.writeBoolean(p.hidden);
    }

    public static PacketSetAppHidden decode(PacketBuffer buf) {
        return new PacketSetAppHidden(buf.readUtf(20), buf.readUtf(64), buf.readBoolean());
    }

    public static void handle(PacketSetAppHidden packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.get().getSender();
            if (sender == null) return;
            if (packet.phoneNumber.isEmpty() || packet.appId.isEmpty()) return;

            ItemStack stack = PhoneItem.findPhoneStack(sender, packet.phoneNumber);
            if (stack.isEmpty()) return;

            PhoneData.setAppHidden(stack, packet.appId, packet.hidden);
            sender.inventory.setChanged();

            PacketHandler.sendToPlayer(
                    new PacketSyncPhone(packet.phoneNumber, PhoneData.getRaw(stack)),
                    sender);
        });
        ctx.get().setPacketHandled(true);
    }
}
