package com.districtlife.phone.network;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye CLIENT -> SERVER pour supprimer un contact du telephone actif. */
public class PacketRemoveContact {

    private final String myPhoneNumber; // numero du telephone ouvert cote client
    private final String contactUuid;

    public PacketRemoveContact(String myPhoneNumber, String contactUuid) {
        this.myPhoneNumber = myPhoneNumber;
        this.contactUuid   = contactUuid;
    }

    public static void encode(PacketRemoveContact p, PacketBuffer buf) {
        buf.writeUtf(p.myPhoneNumber, 20);
        buf.writeUtf(p.contactUuid, 36);
    }

    public static PacketRemoveContact decode(PacketBuffer buf) {
        return new PacketRemoveContact(buf.readUtf(20), buf.readUtf(36));
    }

    public static void handle(PacketRemoveContact packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = PhoneItem.findPhoneStack(player, packet.myPhoneNumber);
            if (stack.isEmpty()) return;

            PhoneData.removeContact(stack, packet.contactUuid);

            player.inventory.setChanged();
            PacketHandler.sendToPlayer(
                    new PacketSyncPhone(packet.myPhoneNumber, PhoneData.getRaw(stack)), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
