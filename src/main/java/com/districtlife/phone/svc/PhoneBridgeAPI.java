package com.districtlife.phone.svc;

import com.districtlife.phone.capability.Conversation;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketSyncPhone;
import com.districtlife.phone.network.PacketSmsNotify;

import java.util.List;

/**
 * API statique appelee par reflexion depuis DLCitizens (PhoneChannelBridge).
 * Toutes les methodes acceptent Object pour eviter les imports NMS dans DLCitizens.
 */
public final class PhoneBridgeAPI {

    private PhoneBridgeAPI() {}

    /**
     * Retourne le numero de telephone de l'item en main ou dans l'inventaire du joueur.
     * @param playerObj ServerPlayerEntity passe en Object depuis DLCitizens
     * @return numero (ex. "0612345678") ou chaine vide si aucun telephone trouve
     */
    public static String getPhoneNumber(Object playerObj) {
        if (!(playerObj instanceof ServerPlayerEntity)) return "";
        ServerPlayerEntity player = (ServerPlayerEntity) playerObj;
        ItemStack stack = PhoneItem.findFirstPhoneStack(player);
        if (stack.isEmpty()) return "";
        return PhoneItem.getPhoneNumber(stack);
    }

    /**
     * Livre une liste de SMS en attente dans le telephone du joueur et synchro le client.
     * Appelee par DLCitizens a la connexion apres avoir marque les SMS comme livres en BDD.
     *
     * @param playerObj      ServerPlayerEntity passe en Object
     * @param receiverPhone  Numero du telephone destinataire
     * @param pendingSms     Liste de {senderPhone, message, timestamp_string}
     */
    public static void deliverPendingSms(Object playerObj, String receiverPhone,
                                         List<String[]> pendingSms) {
        if (!(playerObj instanceof ServerPlayerEntity)) return;
        if (pendingSms == null || pendingSms.isEmpty()) return;

        ServerPlayerEntity player = (ServerPlayerEntity) playerObj;
        ItemStack stack = PhoneItem.findPhoneStack(player, receiverPhone);
        if (stack.isEmpty()) return;

        long now = player.level.getGameTime();
        for (String[] row : pendingSms) {
            if (row.length < 3) continue;
            String senderPhone = row[0];
            String message     = row[1];
            long   timestamp;
            try { timestamp = Long.parseLong(row[2]); } catch (NumberFormatException e) { timestamp = now; }

            Conversation conv = PhoneData.getOrCreateConversation(stack, senderPhone);
            conv.addMessage(new Conversation.Message(message, false, timestamp));
            PhoneData.saveConversation(stack, conv);
        }

        PhoneData.setHasUnreadSMS(stack, true);
        player.inventory.setChanged();

        PacketHandler.sendToPlayer(
                new PacketSyncPhone(receiverPhone, PhoneData.getRaw(stack)), player);
        PacketHandler.sendToPlayer(new PacketSmsNotify(), player);
    }
}
