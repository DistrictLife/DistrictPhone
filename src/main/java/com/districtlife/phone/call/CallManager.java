package com.districtlife.phone.call;

import com.districtlife.phone.block.PhoneFixTileEntity;
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

    private static final Map<String, String>          pending         = new HashMap<>();
    private static final Map<String, String>          active          = new HashMap<>();
    /** Groupes SVC actifs : cle = callerPhone (la partie qui a initie l'appel). */
    private static final Map<String, Group>           callGroups      = new HashMap<>();
    /** Registre des boitiers telephoniques : numero -> TileEntity. */
    private static final Map<String, PhoneFixTileEntity> phoneFixRegistry = new HashMap<>();

    private CallManager() {}

    // -------------------------------------------------------------------------
    // Registre boitiers telephoniques
    // -------------------------------------------------------------------------

    public static void registerFix(String phone, PhoneFixTileEntity te) {
        phoneFixRegistry.put(digitsOnly(phone), te);
    }

    public static void unregisterFix(String phone) {
        phoneFixRegistry.remove(digitsOnly(phone));
    }

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
        if (callee != null && callee != caller) {
            pending.put(callerPhone, calleePhone);
            PacketHandler.sendToPlayer(
                    new PacketCallRequest(callerPhone, caller.getDisplayName().getString()), callee);
            return;
        }

        // Cible = boitier telephonique
        PhoneFixTileEntity calleeFix = getFixTE(calleePhone);
        if (calleeFix != null && calleeFix.isIdle()) {
            pending.put(callerPhone, calleePhone);
            calleeFix.setRinging(callerPhone);
            return;
        }

        PacketHandler.sendToPlayer(
                new PacketCallUpdate(PacketCallUpdate.Signal.UNAVAILABLE, calleePhone, 0), caller);
    }

    // -------------------------------------------------------------------------
    // Accepter
    // -------------------------------------------------------------------------

    public static void acceptCall(String calleePhone, String callerPhone,
                                   ServerPlayerEntity callee, MinecraftServer server) {
        String expected = pending.get(callerPhone);
        if (!calleePhone.equals(expected)) return;
        pending.remove(callerPhone);

        active.put(callerPhone, calleePhone);
        active.put(calleePhone, callerPhone);

        long now = callee.level.getGameTime();
        // Notifie le joueur qui repond (toujours un joueur avec telephone portable)
        PacketHandler.sendToPlayer(
                new PacketCallUpdate(PacketCallUpdate.Signal.ACCEPTED, callerPhone, now), callee);

        ServerPlayerEntity caller = findPlayerByPhone(server, callerPhone);
        if (caller != null) {
            // Appelant = joueur avec telephone portable
            PacketHandler.sendToPlayer(
                    new PacketCallUpdate(PacketCallUpdate.Signal.ACCEPTED, calleePhone, now), caller);

            if (SVCPlugin.isAvailable()) {
                try {
                    Group group = SVCPlugin.SERVER_API.createGroup(
                            "call_" + callerPhone, UUID.randomUUID().toString());
                    callGroups.put(callerPhone, group);
                    VoicechatConnection connCaller =
                            SVCPlugin.SERVER_API.getConnectionOf(caller.getUUID());
                    VoicechatConnection connCallee =
                            SVCPlugin.SERVER_API.getConnectionOf(callee.getUUID());
                    if (connCaller != null) connCaller.setGroup(group);
                    if (connCallee != null) connCallee.setGroup(group);
                } catch (Exception e) {}
            }
        } else {
            // Appelant = boitier telephonique (fix→phone)
            PhoneFixTileEntity callerFix = getFixTE(callerPhone);
            UUID interactorUUID = callerFix != null ? callerFix.getInteractingPlayer() : null;
            if (callerFix != null) callerFix.setInCall(calleePhone, interactorUUID);

            notifyFixInteractor(callerPhone, server,
                    new PacketCallUpdate(PacketCallUpdate.Signal.ACCEPTED, calleePhone, now));

            if (SVCPlugin.isAvailable() && interactorUUID != null) {
                try {
                    ServerPlayerEntity interactor = server.getPlayerList().getPlayer(interactorUUID);
                    if (interactor != null) {
                        Group group = SVCPlugin.SERVER_API.createGroup(
                                "call_" + callerPhone, UUID.randomUUID().toString());
                        callGroups.put(callerPhone, group);
                        VoicechatConnection connI =
                                SVCPlugin.SERVER_API.getConnectionOf(interactorUUID);
                        VoicechatConnection connC =
                                SVCPlugin.SERVER_API.getConnectionOf(callee.getUUID());
                        if (connI != null) connI.setGroup(group);
                        if (connC != null) connC.setGroup(group);
                    }
                } catch (Exception e) {}
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
        } else {
            // Appelant = boitier telephonique
            notifyFixInteractor(callerPhone, server,
                    new PacketCallUpdate(PacketCallUpdate.Signal.DECLINED, calleePhone, 0));
            PhoneFixTileEntity callerFix = getFixTE(callerPhone);
            if (callerFix != null) callerFix.resetCallState();
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

            // Recupere avant tout reset (les interacteurs peuvent etre nullifies apres)
            ServerPlayerEntity mePlayer      = findPlayerByPhone(server, myPhone);
            ServerPlayerEntity partnerPlayer = findPlayerByPhone(server, partner);
            // Pour SVC : si le partenaire est un boitier, son interacteur est le vrai participant
            ServerPlayerEntity partnerSVC = partnerPlayer != null
                    ? partnerPlayer : getFixInteractor(server, partner);

            // Notifie le partenaire (joueur ou boitier)
            if (partnerPlayer != null) {
                PacketHandler.sendToPlayer(
                        new PacketCallUpdate(PacketCallUpdate.Signal.ENDED, myPhone, 0), partnerPlayer);
            } else {
                notifyFixInteractor(partner, server,
                        new PacketCallUpdate(PacketCallUpdate.Signal.ENDED, myPhone, 0));
                PhoneFixTileEntity partnerFix = getFixTE(partner);
                if (partnerFix != null) partnerFix.resetCallState();
            }

            disconnectSVC(myPhone, partner, mePlayer, partnerSVC);
            return;
        }

        // Cas 2 : appel sortant non encore repondu (caller annule)
        String pendingCallee = pending.remove(myPhone);
        if (pendingCallee != null) {
            ServerPlayerEntity calleePlayer = findPlayerByPhone(server, pendingCallee);
            if (calleePlayer != null) {
                PacketHandler.sendToPlayer(
                        new PacketCallUpdate(PacketCallUpdate.Signal.ENDED, myPhone, 0), calleePlayer);
            } else {
                // Callee = boitier telephonique
                PhoneFixTileEntity calleeFix = getFixTE(pendingCallee);
                if (calleeFix != null) calleeFix.resetCallState();
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
                } else {
                    // Caller = boitier telephonique
                    notifyFixInteractor(e.getKey(), server,
                            new PacketCallUpdate(PacketCallUpdate.Signal.DECLINED, myPhone, 0));
                    PhoneFixTileEntity callerFix = getFixTE(e.getKey());
                    if (callerFix != null) callerFix.resetCallState();
                }
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Appels depuis/vers un boitier telephonique
    // -------------------------------------------------------------------------

    /**
     * Appel sortant depuis un boitier.
     * Si la cible est un joueur avec un telephone : chemin classique.
     * Si la cible est un autre boitier : sonne ce boitier.
     */
    public static void initiateCallFromFix(String callerPhone, String calleePhone,
                                            ServerPlayerEntity interactor,
                                            MinecraftServer server) {
        if (callerPhone.equals(calleePhone)) return;
        if (active.containsKey(callerPhone)) return;

        if (active.containsKey(calleePhone) || pending.containsValue(calleePhone)) {
            PacketHandler.sendToPlayer(
                    new PacketCallUpdate(PacketCallUpdate.Signal.BUSY, calleePhone, 0), interactor);
            return;
        }

        // Cible = joueur avec telephone
        ServerPlayerEntity calleePlayer = findPlayerByPhone(server, calleePhone);
        if (calleePlayer != null) {
            pending.put(callerPhone, calleePhone);
            PacketHandler.sendToPlayer(
                    new PacketCallRequest(callerPhone, "Boitier " + callerPhone), calleePlayer);
            return;
        }

        // Cible = boitier telephonique
        PhoneFixTileEntity calleeFix = getFixTE(calleePhone);
        if (calleeFix != null && calleeFix.isIdle()) {
            pending.put(callerPhone, calleePhone);
            calleeFix.setRinging(callerPhone);
            return;
        }

        PacketHandler.sendToPlayer(
                new PacketCallUpdate(PacketCallUpdate.Signal.UNAVAILABLE, calleePhone, 0), interactor);
    }

    /** Acceptation d'un appel entrant sur un boitier. */
    public static void acceptCallToFix(String fixPhone, String callerPhone,
                                        ServerPlayerEntity interactor,
                                        MinecraftServer server) {
        String expected = pending.get(callerPhone);
        if (!fixPhone.equals(expected)) return;
        pending.remove(callerPhone);

        PhoneFixTileEntity fixTE = getFixTE(fixPhone);
        if (fixTE != null) fixTE.setInCall(callerPhone, interactor.getUUID());

        active.put(callerPhone, fixPhone);
        active.put(fixPhone, callerPhone);

        long now = interactor.level.getGameTime();

        // Notifie l'interlocuteur (joueur ou boitier)
        ServerPlayerEntity callerPlayer = findPlayerByPhone(server, callerPhone);
        if (callerPlayer != null) {
            PacketHandler.sendToPlayer(
                    new PacketCallUpdate(PacketCallUpdate.Signal.ACCEPTED, fixPhone, now), callerPlayer);
        } else {
            notifyFixInteractor(callerPhone, server,
                    new PacketCallUpdate(PacketCallUpdate.Signal.ACCEPTED, fixPhone, now));
        }

        // Notifie le joueur qui a decroché le boitier
        PacketHandler.sendToPlayer(
                new PacketCallUpdate(PacketCallUpdate.Signal.ACCEPTED, callerPhone, now), interactor);

        // SVC : groupe vocal entre l'interacteur du boitier et l'appelant
        if (SVCPlugin.isAvailable()) {
            try {
                Group group = SVCPlugin.SERVER_API.createGroup(
                        "call_" + callerPhone, UUID.randomUUID().toString());
                callGroups.put(callerPhone, group);

                VoicechatConnection connInteractor =
                        SVCPlugin.SERVER_API.getConnectionOf(interactor.getUUID());
                if (connInteractor != null) connInteractor.setGroup(group);

                if (callerPlayer != null) {
                    VoicechatConnection connCaller =
                            SVCPlugin.SERVER_API.getConnectionOf(callerPlayer.getUUID());
                    if (connCaller != null) connCaller.setGroup(group);
                } else {
                    // Caller = boitier telephonique (fix→fix) : connecte son interacteur
                    ServerPlayerEntity callerInteractor = getFixInteractor(server, callerPhone);
                    if (callerInteractor != null) {
                        VoicechatConnection connCI =
                                SVCPlugin.SERVER_API.getConnectionOf(callerInteractor.getUUID());
                        if (connCI != null) connCI.setGroup(group);
                    }
                }
            } catch (Exception e) {}
        }
    }

    /** Refus d'un appel entrant sur un boitier. */
    public static void declineCallToFix(String fixPhone, String callerPhone,
                                         MinecraftServer server) {
        String expected = pending.get(callerPhone);
        if (!fixPhone.equals(expected)) return;
        pending.remove(callerPhone);

        PhoneFixTileEntity fixTE = getFixTE(fixPhone);
        if (fixTE != null) fixTE.resetCallState();

        ServerPlayerEntity callerPlayer = findPlayerByPhone(server, callerPhone);
        if (callerPlayer != null) {
            PacketHandler.sendToPlayer(
                    new PacketCallUpdate(PacketCallUpdate.Signal.DECLINED, fixPhone, 0), callerPlayer);
        } else {
            notifyFixInteractor(callerPhone, server,
                    new PacketCallUpdate(PacketCallUpdate.Signal.DECLINED, fixPhone, 0));
        }
    }

    /** Raccrocher depuis un boitier (appel actif ou sonnerie). */
    public static void hangupFix(String fixPhone, MinecraftServer server) {
        PhoneFixTileEntity fixTE = getFixTE(fixPhone);

        // Cas 1 : appel actif
        String partner = active.remove(fixPhone);
        if (partner != null) {
            active.remove(partner);

            // Recupere les interacteurs avant tout reset
            ServerPlayerEntity fixInteractor = getFixInteractor(server, fixPhone);
            ServerPlayerEntity partnerPlayer = findPlayerByPhone(server, partner);
            ServerPlayerEntity partnerSVC    = partnerPlayer != null
                    ? partnerPlayer : getFixInteractor(server, partner);

            if (fixTE != null) fixTE.resetCallState();

            if (partnerPlayer != null) {
                PacketHandler.sendToPlayer(
                        new PacketCallUpdate(PacketCallUpdate.Signal.ENDED, fixPhone, 0), partnerPlayer);
            } else {
                PhoneFixTileEntity partnerFix = getFixTE(partner);
                if (partnerFix != null) {
                    notifyFixInteractor(partner, server,
                            new PacketCallUpdate(PacketCallUpdate.Signal.ENDED, fixPhone, 0));
                    partnerFix.resetCallState();
                }
            }

            disconnectSVC(fixPhone, partner, fixInteractor, partnerSVC);
            return;
        }

        // Cas 2 : appel sortant non repondu (le boitier annule)
        String pendingCallee = pending.remove(fixPhone);
        if (pendingCallee != null) {
            if (fixTE != null) fixTE.resetCallState();
            ServerPlayerEntity calleePlayer = findPlayerByPhone(server, pendingCallee);
            if (calleePlayer != null) {
                PacketHandler.sendToPlayer(
                        new PacketCallUpdate(PacketCallUpdate.Signal.ENDED, fixPhone, 0), calleePlayer);
            } else {
                PhoneFixTileEntity calleeFix = getFixTE(pendingCallee);
                if (calleeFix != null) calleeFix.resetCallState();
            }
            return;
        }

        // Cas 3 : appel entrant non repondu (le boitier ignore l'appel)
        Iterator<Map.Entry<String, String>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getValue().equals(fixPhone)) {
                it.remove();
                if (fixTE != null) fixTE.resetCallState();
                ServerPlayerEntity callerPlayer = findPlayerByPhone(server, e.getKey());
                if (callerPlayer != null) {
                    PacketHandler.sendToPlayer(
                            new PacketCallUpdate(PacketCallUpdate.Signal.DECLINED, fixPhone, 0), callerPlayer);
                }
                break;
            }
        }
    }

    /** Envoie un paquet au joueur interagissant avec un boitier (s'il existe). */
    private static void notifyFixInteractor(String fixPhone, MinecraftServer server, Object packet) {
        PhoneFixTileEntity fix = getFixTE(fixPhone);
        if (fix == null) return;
        UUID uuid = fix.getInteractingPlayer();
        if (uuid == null) return;
        ServerPlayerEntity player = server.getPlayerList().getPlayer(uuid);
        if (player != null) PacketHandler.sendToPlayer(packet, player);
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

    /** Retourne le joueur interagissant avec un boitier, ou null. */
    private static ServerPlayerEntity getFixInteractor(MinecraftServer server, String fixPhone) {
        PhoneFixTileEntity fix = getFixTE(fixPhone);
        if (fix == null) return null;
        UUID uuid = fix.getInteractingPlayer();
        return uuid != null ? server.getPlayerList().getPlayer(uuid) : null;
    }

    /** Supprime tous les caracteres non numeriques ("06 12 34 56 78" → "0612345678"). */
    private static String digitsOnly(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    /** Retourne la TileEntity boitier associee a un numero (normalise), ou null. */
    private static PhoneFixTileEntity getFixTE(String phone) {
        return phoneFixRegistry.get(digitsOnly(phone));
    }
}
