package com.districtlife.phone.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** SERVER -> CLIENT : ouvre l'ecran du boitier telephonique. */
public class PacketOpenPhoneFix {

    public final String   phoneNumber;
    public final String   pendingCaller;
    public final String   activeCall;
    public final BlockPos blockPos;

    public PacketOpenPhoneFix(String phoneNumber, String pendingCaller,
                               String activeCall, BlockPos blockPos) {
        this.phoneNumber   = phoneNumber;
        this.pendingCaller = pendingCaller;
        this.activeCall    = activeCall;
        this.blockPos      = blockPos;
    }

    public static void encode(PacketOpenPhoneFix p, PacketBuffer buf) {
        buf.writeUtf(p.phoneNumber,   20);
        buf.writeUtf(p.pendingCaller, 20);
        buf.writeUtf(p.activeCall,    20);
        buf.writeBlockPos(p.blockPos);
    }

    public static PacketOpenPhoneFix decode(PacketBuffer buf) {
        return new PacketOpenPhoneFix(
                buf.readUtf(20), buf.readUtf(20), buf.readUtf(20), buf.readBlockPos());
    }

    public static void handle(PacketOpenPhoneFix packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                MinecraftForge.EVENT_BUS.post(new PhoneNetEvent.OpenPhoneFix(
                        packet.phoneNumber, packet.pendingCaller,
                        packet.activeCall, packet.blockPos)));
        ctx.get().setPacketHandled(true);
    }
}
