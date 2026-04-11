package com.districtlife.phone.svc;

import com.districtlife.phone.PhoneMod;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

/**
 * Plugin Simple Voice Chat.
 * Decouverte automatique par Forge via le scan des annotations @ForgeVoicechatPlugin.
 * Si SVC n'est pas installe, cette classe n'est jamais chargee.
 */
@ForgeVoicechatPlugin
public class SVCPlugin implements VoicechatPlugin {

    /** API serveur SVC, null si SVC n'est pas actif. */
    public static volatile VoicechatServerApi SERVER_API = null;

    @Override
    public String getPluginId() {
        return PhoneMod.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        // Rien ici — on recupere l'API via l'evenement ServerStarted
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
            SERVER_API = event.getVoicechat();
            SVCBridge.setAvailable(true);
        });

        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
            SERVER_API = null;
            SVCBridge.setAvailable(false);
        });
    }

    /** Retourne true si SVC est charge et actif cote serveur. */
    public static boolean isAvailable() {
        return SERVER_API != null;
    }
}
