package com.districtlife.phone.call;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Etat client-side de l'appel en cours.
 * Partage entre AppPhone et PhoneCallHud via champs statiques.
 */
@OnlyIn(Dist.CLIENT)
public final class PhoneCallState {

    public enum CallState { IDLE, CALLING, RINGING, INCALL, IMPOSSIBLE }

    private static CallState state           = CallState.IDLE;
    private static String    otherPhone      = "";
    private static String    otherMcName     = "";  // nom Minecraft de l'autre joueur
    private static long      callStartTick   = 0;
    private static int       impossibleTicks = 0;

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

    /** Appel impossible (meme joueur ou cible deconnectee). Auto-reset apres 3 secondes. */
    public static void setImpossible() {
        state           = CallState.IMPOSSIBLE;
        otherPhone      = "";
        otherMcName     = "";
        callStartTick   = 0;
        impossibleTicks = 60;
    }

    /** Decremente le timer IMPOSSIBLE et reinitialise quand il expire. Appeler depuis tick(). */
    public static void tickImpossible() {
        if (state == CallState.IMPOSSIBLE) {
            impossibleTicks--;
            if (impossibleTicks <= 0) reset();
        }
    }

    /** Fin ou annulation d'appel. */
    public static void reset() {
        state           = CallState.IDLE;
        otherPhone      = "";
        otherMcName     = "";
        callStartTick   = 0;
        impossibleTicks = 0;
    }

    public static boolean isIdle() { return state == CallState.IDLE; }
}
