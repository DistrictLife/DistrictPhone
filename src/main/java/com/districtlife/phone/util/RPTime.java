package com.districtlife.phone.util;

/**
 * Calcule l'heure RP et la date RP a partir des ticks monde Minecraft.
 *
 * 1 jour Minecraft = 24 000 ticks = 24h RP
 * Tick 0 -> 6h00 du matin
 * Formule heure : (dayTime % 24000 + 6000) % 24000 / 1000.0
 */
public class RPTime {

    private static final String[] DAYS_OF_WEEK = {
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };

    private static final String[] MONTHS = {
            "Janvier", "Fevrier", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Decembre"
    };

    private final int hours;
    private final int minutes;
    private final long absoluteDay; // jour absolu depuis le tick 0

    public RPTime(long worldDayTime) {
        long tickOfDay = worldDayTime % 24000;
        long totalMinutes = (long) ((tickOfDay + 6000) % 24000 / 1000.0 * 60);
        this.hours = (int) (totalMinutes / 60) % 24;
        this.minutes = (int) (totalMinutes % 60);
        this.absoluteDay = worldDayTime / 24000;
    }

    /** Retourne l'heure RP formatee : "HH:mm" */
    public String getFormattedTime() {
        return String.format("%02d:%02d", hours, minutes);
    }

    /** Retourne le nom du jour de la semaine (cycle de 7) */
    public String getDayName() {
        return DAYS_OF_WEEK[(int) (absoluteDay % 7)];
    }

    /** Retourne le numero du jour dans le mois RP (cycle de 30) */
    public int getDayOfMonth() {
        return (int) (absoluteDay % 30) + 1;
    }

    /** Retourne le nom du mois RP (cycle de 12) */
    public String getMonthName() {
        int monthIndex = (int) ((absoluteDay / 30) % 12);
        return MONTHS[monthIndex];
    }

    /** Retourne l'annee RP (An 1, An 2, ...) */
    public int getYear() {
        return (int) (absoluteDay / 360) + 1;
    }

    /**
     * Retourne la date RP formatee : "Lundi 14 Avril - An 1"
     */
    public String getFormattedDate() {
        return getDayName() + " " + getDayOfMonth() + " " + getMonthName() + " - An " + getYear();
    }

    public int getHours() { return hours; }
    public int getMinutes() { return minutes; }
    public long getAbsoluteDay() { return absoluteDay; }
}
