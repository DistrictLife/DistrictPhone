package com.districtlife.phone.call;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Journal d'appels cote client (50 entrees max, plus recente en tete).
 */
@OnlyIn(Dist.CLIENT)
public class CallLogClient {

    private static final int MAX = 50;
    private static final List<CallLogEntry> entries = new ArrayList<>();

    public static void add(CallLogEntry entry) {
        entries.add(0, entry);
        if (entries.size() > MAX) entries.remove(MAX);
    }

    public static List<CallLogEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
