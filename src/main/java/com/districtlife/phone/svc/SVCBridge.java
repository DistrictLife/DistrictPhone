package com.districtlife.phone.svc;

/**
 * Pont sans import SVC.
 *
 * Permet a CallManager (et tout autre code) de tester la disponibilite de
 * Simple Voice Chat sans charger SVCPlugin.class, qui elle-meme necessite
 * VoicechatPlugin et crasherait si SVC est absent.
 *
 * SVCPlugin positionne ce flag via setAvailable() depuis ses event-handlers.
 */
public final class SVCBridge {

    private SVCBridge() {}

    private static volatile boolean available = false;

    /** Appele par SVCPlugin quand le serveur SVC demarre/s'arrete. */
    static void setAvailable(boolean value) {
        available = value;
    }

    /** True si Simple Voice Chat est charge et actif cote serveur. */
    public static boolean isAvailable() {
        return available;
    }
}
