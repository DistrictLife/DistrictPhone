package com.districtlife.phone.call;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Entree du journal d'appels cote client.
 */
@OnlyIn(Dist.CLIENT)
public class CallLogEntry {

    public enum Type { OUTGOING, INCOMING, MISSED }

    public final String number;
    public final String displayName;
    public final Type   type;
    public final long   gameTick;

    public CallLogEntry(String number, String displayName, Type type, long gameTick) {
        this.number      = number;
        this.displayName = displayName;
        this.type        = type;
        this.gameTick    = gameTick;
    }
}
