package com.districtlife.phone.network;

import com.districtlife.phone.capability.Contact;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Envoye CLIENT -> SERVER pour ajouter un contact au telephone actif. */
public class PacketAddContact {

    private final String myPhoneNumber; // numero du telephone ouvert cote client
    private final String pseudo;
    private final String contactPhone;

    public PacketAddContact(String myPhoneNumber, String pseudo, String contactPhone) {
        this.myPhoneNumber = myPhoneNumber;
        this.pseudo        = pseudo;
        this.contactPhone  = contactPhone;
    }

    public static void encode(PacketAddContact p, PacketBuffer buf) {
        buf.writeUtf(p.myPhoneNumber, 20);
        buf.writeUtf(p.pseudo, 64);
        buf.writeUtf(p.contactPhone, 20);
    }

    public static PacketAddContact decode(PacketBuffer buf) {
        return new PacketAddContact(buf.readUtf(20), buf.readUtf(64), buf.readUtf(20));
    }

    public static void handle(PacketAddContact packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = PhoneItem.findPhoneStack(player, packet.myPhoneNumber);
            if (stack.isEmpty()) return;

            PhoneData.addContact(stack, new Contact(UUID.randomUUID(), packet.pseudo, packet.contactPhone));

            player.inventory.setChanged();
            PacketHandler.sendToPlayer(
                    new PacketSyncPhone(packet.myPhoneNumber, PhoneData.getRaw(stack)), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
