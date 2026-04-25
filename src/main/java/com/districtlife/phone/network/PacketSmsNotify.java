package com.districtlife.phone.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Notification legere SERVER->CLIENT : un SMS vient d'etre recu. */
public class PacketSmsNotify {

    public static void encode(PacketSmsNotify p, PacketBuffer buf) {}

    public static PacketSmsNotify decode(PacketBuffer buf) { return new PacketSmsNotify(); }

    public static void handle(PacketSmsNotify packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                MinecraftForge.EVENT_BUS.post(new PhoneNetEvent.SmsNotify()));
        ctx.get().setPacketHandled(true);
    }
}
