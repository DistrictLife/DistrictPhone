package com.districtlife.phone.network;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye SERVER -> CLIENT pour synchroniser les donnees du telephone. */
public class PacketSyncPhone {

    private final String phoneNumber;
    private final CompoundNBT data;

    public PacketSyncPhone(String phoneNumber, CompoundNBT data) {
        this.phoneNumber = phoneNumber;
        this.data        = data;
    }

    public static void encode(PacketSyncPhone packet, PacketBuffer buf) {
        buf.writeUtf(packet.phoneNumber, 20);
        buf.writeNbt(packet.data);
    }

    public static PacketSyncPhone decode(PacketBuffer buf) {
        return new PacketSyncPhone(buf.readUtf(20), buf.readNbt());
    }

    public static void handle(PacketSyncPhone packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            // Trouve le telephone avec ce numero dans l'inventaire local et met a jour ses donnees
            ItemStack stack = PhoneItem.findPhoneStack(
                    Minecraft.getInstance().player, packet.phoneNumber);
            if (!stack.isEmpty()) {
                PhoneData.setRaw(stack, packet.data);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
