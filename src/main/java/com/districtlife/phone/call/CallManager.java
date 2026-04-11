package com.districtlife.phone.call;

import com.districtlife.phone.item.PhoneItem;
import com.districtlife.phone.network.PacketCallRequest;
import com.districtlife.phone.network.PacketCallUpdate;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.svc.SVCPlugin;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion server-side des appels.
 *
 * pending     : callerPhone -> calleePhone  (sonnerie, pas encore repondu)
 * active      : phone -> partnerPhone       (appel actif, entrees bidirectionnelles)
 * callGroups  : callerPhone -> Group SVC    (groupe vocal cree pour l'appel)
 */
public final class CallManager {

    private static final Map<String, String> pending    = new HashMap<>();
    private static final Map<String, String> active     = new HashMap<>();
    /** Groupes SVC actifs : cle = callerPhone (la partie qui a initie l'appel). */
    private static final Map<String, Group>  callGroups = new HashMap<>();

    private CallManager() {}

    // -------------------------------------------------------------------------
    // Initier un appel
    // -------------------------------------------------------------------------

    public static void initiateCall(String callerPhone, String calleePhone,
                                    ServerPlayerEntity caller, MinecraftServer server) {
        if (callerPhone.equals(calleePhone)) return;
        if (active.containsKey(callerPhone)) return;

        if (active.containsKey(calleePhone) || pending.containsValue(calleePhone)) {
            PacketHandler.sendToPlayer(
                    new PacketCallUpdate(PacketCallUpdate.Signal.BUSY, calleePhone, 0), caller);
            return;
        }

        ServerPlayerEntity callee = findPlayerByPhone(server, calleePhone);
        if (callee == null) {
            PacketHandler.sendToPlayer(
                    new PacketCallUpdate(PacketCallUpdate.Signal.UNAVAILABLE, calleePhone, 0), caller);
            return;
        }

        pending.put(callerPhone, calleePhone);
        PacketHandler.sendToPlayer(
                new PacketCallRequest(callerPhone, caller.getDisplayName().getString()), callee);
    }

    // -------------------------------------------------------------------------
    // Accepter
    // -------------------------------------------------------------------------

    public static void acceptCall(String calleePhone, String callerPhone,
                                   ServerPlayerEntity callee, MinecraftServer server) {
        String expected = pending.get(callerPhone);
        if (!calleePhone.equals(expected)) return;

        ServerPlayerEntity caller = findPlayerByPhone(server, callerPhone);
        pending.remove(callerPhone);
        if (caller == null) return;

        active.put(callerPhone, calleePhone);
        active.put(calleePhone, callerPhone);

        long now = callee.level.getGameTime();
        PacketHandler.sendToPlayer(
                new PacketCallUpdate(PacketCallUpdate.Signal.ACCEPTED, calleePhone, now), caller);
        PacketHandler.sendToPlayer(
                new PacketCallUpdate(PacketCallUpdate.Signal.ACCEPTED, callerPhone, now), callee);

        // --- SVC : creer un groupe vocal prive pour l'appel ---
        if (SVCPlugin.isAvailable()) {
            try {
                // Mot de passe aleatoire pour que personne d'autre ne puisse rejoindre
                Group group = SVCPlugin.SERVER_API.createGroup(
                        "call_" + callerPhone,
                        UUID.randomUUID().toString());
                callGroups.put(callerPhone, group);

                VoicechatConnection connCaller =
                        SVCPlugin.SERVER_API.getConnectionOf(caller.getUUID());
                VoicechatConnection connCallee =
                        SVCPlugin.SERVER_API.getConnectionOf(callee.getUUID());

                if (connCaller != null) connCaller.setGroup(group);
                if (connCallee != null) connCallee.setGroup(group);
            } catch (Exception e) {
                // SVC disponible mais erreur inattendue — l'appel continue sans voix groupee
            }
        }
    }

    // -------------------------------------------------------------------------
    // Refuser
    // -------------------------------------------------------------------------

    public static void declineCall(String calleePhone, String callerPhone,
                                    MinecraftServer server) {
        String expected = pending.get(callerPhone);
        if (!calleePhone.equals(expected)) return;
        pending.remove(callerPhone);

        ServerPlayerEntity caller = findPlayerByPhone(server, callerPhone);
        if (caller != null) {
            PacketHandler.sendToPlayer(
                    new PacketCallUpdate(PacketCallUpdate.Signal.DECLINED, calleePhone, 0), caller);
        }
    }

    // -------------------------------------------------------------------------
    // Raccrocher
    // -------------------------------------------------------------------------

    public static void hangup(String myPhone, MinecraftServer server) {

        // Cas 1 : appel actif
        String partner = active.remove(myPhone);
        if (partner != null) {
            active.remove(partner);

            // Retrouve les UUIDs pour SVC avant de nettoyer
            ServerPlayerEntity mePlayer      = findPlayerByPhone(server, myPhone);
            ServerPlayerEntity partnerPlayer = findPlayerByPhone(server, partner);

            // Notifie le partenaire
            if (partnerPlayer != null) {
                PacketHandler.sendToPlayer(
                        new PacketCallUpdate(PacketCallUpdate.Signal.ENDED, myPhone, 0), partnerPlayer);
            }

            // SVC : retire les deux joueurs du groupe vocal et supprime le groupe
            disconnectSVC(myPhone, partner, mePlayer, partnerPlayer);
            return;
        }

        // Cas 2 : appel sortant non encore repondu (caller annule)
        String pendingCallee = pending.remove(myPhone);
        if (pendingCallee != null) {
            ServerPlayerEntity calleePlayer = findPlayerByPhone(server, pendingCallee);
            if (calleePlayer != null) {
                PacketHandler.sendToPlayer(
                        new PacketCallUpdate(PacketCallUpdate.Signal.ENDED, myPhone, 0), calleePlayer);
            }
            return;
        }

        // Cas 3 : appel entrant non repondu (callee rejette en raccrochant)
        Iterator<Map.Entry<String, String>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getValue().equals(myPhone)) {
                it.remove();
                ServerPlayerEntity callerPlayer = findPlayerByPhone(server, e.getKey());
                if (callerPlayer != null) {
                    PacketHandler.sendToPlayer(
                            new PacketCallUpdate(PacketCallUpdate.Signal.DECLINED, myPhone, 0),
                            callerPlayer);
                }
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // SVC helpers
    // -------------------------------------------------------------------------

    /**
     * Retire les deux joueurs de leur groupe SVC et supprime la reference au groupe.
     * La cle peut etre dans callGroups sous myPhone OU sous partner (selon qui a initie).
     */
    private static void disconnectSVC(String myPhone, String partner,
                                       ServerPlayerEntity mePlayer,
                                       ServerPlayerEntity partnerPlayer) {
        if (!SVCPlugin.isAvailable()) return;

        Group group = callGroups.remove(myPhone);
        if (group == null) group = callGroups.remove(partner);
        if (group == null) return;

        try {
            if (mePlayer != null) {
                VoicechatConnection conn =
                        SVCPlugin.SERVER_API.getConnectionOf(mePlayer.getUUID());
                if (conn != null) conn.setGroup(null);
            }
            if (partnerPlayer != null) {
                VoicechatConnection conn =
                        SVCPlugin.SERVER_API.getConnectionOf(partnerPlayer.getUUID());
                if (conn != null) conn.setGroup(null);
            }
        } catch (Exception e) {
            // Ignore si SVC a deja nettoye la connexion
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static ServerPlayerEntity findPlayerByPhone(MinecraftServer server, String phone) {
        for (ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
            if (!PhoneItem.findPhoneStack(p, phone).isEmpty()) return p;
        }
        return null;
    }
}
