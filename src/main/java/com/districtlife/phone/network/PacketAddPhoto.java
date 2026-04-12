package com.districtlife.phone.network;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye CLIENT -> SERVER pour enregistrer le chemin d'une photo prise. */
public class PacketAddPhoto {

    private final String phoneNumber;
    private final String filename;

    public PacketAddPhoto(String phoneNumber, String filename) {
        this.phoneNumber = phoneNumber;
        this.filename    = filename;
    }

    public static void encode(PacketAddPhoto p, PacketBuffer buf) {
        buf.writeUtf(p.phoneNumber, 20);
        buf.writeUtf(p.filename, 256);
    }

    public static PacketAddPhoto decode(PacketBuffer buf) {
        return new PacketAddPhoto(buf.readUtf(20), buf.readUtf(256));
    }

    public static void handle(PacketAddPhoto packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.get().getSender();
            if (sender == null) return;
            if (packet.phoneNumber.isEmpty() || packet.filename.isEmpty()) return;

            ItemStack stack = PhoneItem.findPhoneStack(sender, packet.phoneNumber);
            if (stack.isEmpty()) return;

            PhoneData.addPhotoPath(stack, packet.filename);
            sender.inventory.setChanged();

            PacketHandler.sendToPlayer(
                    new PacketSyncPhone(packet.phoneNumber, PhoneData.getRaw(stack)),
                    sender);
        });
        ctx.get().setPacketHandled(true);
    }
}
