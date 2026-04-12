package com.districtlife.phone.network;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye SERVER -> CLIENT pour synchroniser les donnees du telephone. */
public class PacketSyncPhone {

    final String      phoneNumber;
    final CompoundNBT data;

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
        ctx.get().enqueueWork(() ->
            MinecraftForge.EVENT_BUS.post(
                new PhoneNetEvent.SyncPhone(packet.phoneNumber, packet.data))
        );
        ctx.get().setPacketHandled(true);
    }
}
