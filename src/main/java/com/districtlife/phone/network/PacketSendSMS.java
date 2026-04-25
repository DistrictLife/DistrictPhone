package com.districtlife.phone.network;

import com.districtlife.phone.capability.Conversation;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye CLIENT -> SERVER pour envoyer un SMS depuis le telephone actif. */
public class PacketSendSMS {

    private final String senderPhoneNumber; // numero du telephone ouvert cote client
    private final String targetPhoneNumber;
    private final String messageText;

    public PacketSendSMS(String senderPhoneNumber, String targetPhoneNumber, String messageText) {
        this.senderPhoneNumber = senderPhoneNumber;
        this.targetPhoneNumber = targetPhoneNumber;
        this.messageText       = messageText;
    }

    public static void encode(PacketSendSMS p, PacketBuffer buf) {
        buf.writeUtf(p.senderPhoneNumber, 20);
        buf.writeUtf(p.targetPhoneNumber, 20);
        buf.writeUtf(p.messageText, 256);
    }

    public static PacketSendSMS decode(PacketBuffer buf) {
        return new PacketSendSMS(buf.readUtf(20), buf.readUtf(20), buf.readUtf(256));
    }

    public static void handle(PacketSendSMS packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.get().getSender();
            if (sender == null) return;
            if (packet.senderPhoneNumber.isEmpty() || packet.targetPhoneNumber.isEmpty()) return;
            if (packet.messageText.trim().isEmpty()) return;

            // Trouve le telephone expediteur par son numero (fonctionne meme si c'est le telephone d'un autre joueur)
            ItemStack senderStack = PhoneItem.findPhoneStack(sender, packet.senderPhoneNumber);
            if (senderStack.isEmpty()) return;

            long timestamp = sender.level.getGameTime();
            String text = packet.messageText.trim();

            // Ajoute le message sortant dans le telephone expediteur
            Conversation senderConv = PhoneData.getOrCreateConversation(senderStack, packet.targetPhoneNumber);
            senderConv.addMessage(new Conversation.Message(text, true, timestamp));
            PhoneData.saveConversation(senderStack, senderConv);

            sender.inventory.setChanged();
            PacketHandler.sendToPlayer(
                    new PacketSyncPhone(packet.senderPhoneNumber, PhoneData.getRaw(senderStack)),
                    sender);

            // Livre le message au destinataire s'il est connecte
            MinecraftServer server = sender.getServer();
            if (server == null) return;
            ServerPlayerEntity target = findPlayerHoldingPhone(server, packet.targetPhoneNumber);
            if (target == null) return;

            ItemStack targetStack = PhoneItem.findPhoneStack(target, packet.targetPhoneNumber);
            if (targetStack.isEmpty()) return;

            Conversation targetConv = PhoneData.getOrCreateConversation(targetStack, packet.senderPhoneNumber);
            targetConv.addMessage(new Conversation.Message(text, false, timestamp));
            PhoneData.saveConversation(targetStack, targetConv);
            PhoneData.setHasUnreadSMS(targetStack, true);

            target.inventory.setChanged();
            PacketHandler.sendToPlayer(
                    new PacketSyncPhone(packet.targetPhoneNumber, PhoneData.getRaw(targetStack)),
                    target);
            PacketHandler.sendToPlayer(new PacketSmsNotify(), target);
        });
        ctx.get().setPacketHandled(true);
    }

    /** Trouve le joueur connecte qui possede le telephone avec ce numero. */
    private static ServerPlayerEntity findPlayerHoldingPhone(MinecraftServer server, String phoneNumber) {
        for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
            if (!PhoneItem.findPhoneStack(p, phoneNumber).isEmpty()) return p;
        }
        return null;
    }
}
