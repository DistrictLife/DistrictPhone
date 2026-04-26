package com.districtlife.phone.network;

import com.districtlife.phone.block.PhoneFixTileEntity;
import com.districtlife.phone.call.CallManager;
import com.districtlife.phone.call.CallSignal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** CLIENT -> SERVER : signaux d'appel depuis un boitier telephonique. */
public class PacketCallSignalFix {

    private final CallSignal signal;
    private final String     fixPhone;
    private final String     targetPhone;
    private final BlockPos   blockPos;

    public PacketCallSignalFix(CallSignal signal, String fixPhone,
                                String targetPhone, BlockPos blockPos) {
        this.signal      = signal;
        this.fixPhone    = fixPhone;
        this.targetPhone = targetPhone;
        this.blockPos    = blockPos;
    }

    public static void encode(PacketCallSignalFix p, PacketBuffer buf) {
        buf.writeByte(p.signal.getId());
        buf.writeUtf(p.fixPhone,    20);
        buf.writeUtf(p.targetPhone, 20);
        buf.writeBlockPos(p.blockPos);
    }

    public static PacketCallSignalFix decode(PacketBuffer buf) {
        return new PacketCallSignalFix(
                CallSignal.fromId(buf.readByte()),
                buf.readUtf(20), buf.readUtf(20), buf.readBlockPos());
    }

    public static void handle(PacketCallSignalFix packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.get().getSender();
            if (sender == null) return;

            // Verifie que le joueur est proche du boitier et que le numero correspond
            TileEntity te = sender.level.getBlockEntity(packet.blockPos);
            if (!(te instanceof PhoneFixTileEntity)) return;
            PhoneFixTileEntity fixTE = (PhoneFixTileEntity) te;
            if (!fixTE.getPhoneNumber().equals(packet.fixPhone)) return;
            if (sender.blockPosition().distSqr(packet.blockPos) > 64) return; // 8 blocs max

            MinecraftServer server = sender.getServer();
            if (server == null) return;

            switch (packet.signal) {
                case CALL:
                    fixTE.setInteractingPlayer(sender.getUUID());
                    CallManager.initiateCallFromFix(
                            packet.fixPhone, packet.targetPhone, sender, server);
                    break;
                case ACCEPT:
                    CallManager.acceptCallToFix(
                            packet.fixPhone, packet.targetPhone, sender, server);
                    break;
                case DECLINE:
                    CallManager.declineCallToFix(
                            packet.fixPhone, packet.targetPhone, server);
                    break;
                case HANGUP:
                    CallManager.hangupFix(packet.fixPhone, server);
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
