package com.districtlife.phone.call;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Etat client-side de l'appel en cours.
 * Partage entre AppPhone et PhoneCallHud via champs statiques.
 */
@OnlyIn(Dist.CLIENT)
public final class PhoneCallState {

    public enum CallState { IDLE, CALLING, RINGING, INCALL }

    private static CallState state           = CallState.IDLE;
    private static String    otherPhone      = "";
    private static String    otherMcName     = "";  // nom Minecraft de l'autre joueur
    private static long      callStartTick   = 0;

    private PhoneCallState() {}

    public static CallState getState()        { return state; }
    public static String    getOtherPhone()   { return otherPhone; }
    public static String    getOtherMcName()  { return otherMcName; }
    public static long      getCallStartTick(){ return callStartTick; }

    /** Appel sortant initie. */
    public static void setCalling(String phone, String mcName) {
        state        = CallState.CALLING;
        otherPhone   = phone;
        otherMcName  = mcName;
        callStartTick = 0;
    }

    /** Appel entrant recu. */
    public static void setRinging(String phone, String mcName) {
        state        = CallState.RINGING;
        otherPhone   = phone;
        otherMcName  = mcName;
        callStartTick = 0;
    }

    /** Les deux parties sont connectees. */
    public static void setInCall(long startTick) {
        state         = CallState.INCALL;
        callStartTick = startTick;
    }

    /** Fin ou annulation d'appel. */
    public static void reset() {
        state         = CallState.IDLE;
        otherPhone    = "";
        otherMcName   = "";
        callStartTick = 0;
    }

    public static boolean isIdle() { return state == CallState.IDLE; }
}
