package com.districtlife.phone.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketOpenDebugTexture {

    public final String texturePath;

    public PacketOpenDebugTexture(String texturePath) {
        this.texturePath = texturePath;
    }

    public static void encode(PacketOpenDebugTexture p, PacketBuffer buf) {
        buf.writeUtf(p.texturePath, 256);
    }

    public static PacketOpenDebugTexture decode(PacketBuffer buf) {
        return new PacketOpenDebugTexture(buf.readUtf(256));
    }

    public static void handle(PacketOpenDebugTexture packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new PhoneNetEvent.OpenDebugTexture(packet.texturePath))
        );
        ctx.get().setPacketHandled(true);
    }
}
