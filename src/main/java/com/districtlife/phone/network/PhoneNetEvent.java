package com.districtlife.phone.network;

import com.districtlife.phone.news.NewsArticle;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

/**
 * Evenements reseau tires par les paquets SERVER->CLIENT.
 * Classes plain (aucun @OnlyIn) : peuvent etre referencees depuis n'importe quelle classe.
 * Le traitement Minecraft est dans PhoneClientHandler, jamais charge sur serveur.
 */
public final class PhoneNetEvent {

    private PhoneNetEvent() {}

    public static class SyncPhone extends Event {
        public final String     phoneNumber;
        public final CompoundNBT data;
        public SyncPhone(String phoneNumber, CompoundNBT data) {
            this.phoneNumber = phoneNumber;
            this.data        = data;
        }
    }

    public static class CallRequest extends Event {
        public final String callerPhone;
        public final String callerMcName;
        public CallRequest(String callerPhone, String callerMcName) {
            this.callerPhone  = callerPhone;
            this.callerMcName = callerMcName;
        }
    }

    public static class CallUpdate extends Event {
        public final PacketCallUpdate.Signal signal;
        public final long                    callStartTick;
        public CallUpdate(PacketCallUpdate.Signal signal, long callStartTick) {
            this.signal        = signal;
            this.callStartTick = callStartTick;
        }
    }

    /** Un nouvel article a ete publie (broadcast en temps reel). */
    public static class ReceiveNews extends Event {
        public final NewsArticle article;
        public ReceiveNews(NewsArticle article) { this.article = article; }
    }

    /** Synchronisation initiale a la connexion : liste complete des articles. */
    public static class SyncNews extends Event {
        public final List<NewsArticle> articles;
        public SyncNews(List<NewsArticle> articles) { this.articles = articles; }
    }

    /** Demande d'ouverture du viewer de texture de debug (OP uniquement). */
    public static class OpenDebugTexture extends Event {
        public final String texturePath;
        public OpenDebugTexture(String texturePath) { this.texturePath = texturePath; }
    }

    /** Un SMS entrant vient d'etre recu. */
    public static class SmsNotify extends Event {}
}
