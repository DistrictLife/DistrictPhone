package com.districtlife.phone.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PhoneFont {

    public static int draw(MatrixStack ms, String text, float x, float y, int color) {
        return fr().draw(ms, text, x, y, color);
    }

    public static int drawShadow(MatrixStack ms, String text, float x, float y, int color) {
        return fr().drawShadow(ms, text, x, y, color);
    }

    public static int width(String text) {
        return fr().width(text);
    }

    public static void drawCentered(MatrixStack ms, String text, int zoneX, float y,
                                    int zoneWidth, int color) {
        float tx = zoneX + (zoneWidth - width(text)) / 2.0F;
        draw(ms, text, tx, y, color);
    }

    /** Largeur sans le +1px inter-caractere de Minecraft (pour grands textes scales). */
    public static int widthNoGap(String text) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            total += Math.max(0, fr().width(text.substring(i, i + 1)) - 1);
        }
        return total;
    }

    /** Rendu caractere par caractere sans le +1px inter-caractere de Minecraft. */
    public static void drawNoGap(MatrixStack ms, String text, float x, float y, int color) {
        drawNoGap(ms, text, x, y, 0, color);
    }

    /**
     * Rendu caractere par caractere avec espacement custom (en pixels, dans l'espace scale actuel).
     * spacing=0 : pas de gap. spacing=1 : 1px supplementaire entre chaque caractere.
     */
    public static void drawNoGap(MatrixStack ms, String text, float x, float y, float spacing, int color) {
        float cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            fr().draw(ms, ch, cx, y, color);
            cx += Math.max(0, fr().width(ch) - 1) + spacing;
        }
    }

    /** Largeur avec espacement custom (meme logique que drawNoGap). */
    public static int widthNoGap(String text, float spacing) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            total += Math.max(0, fr().width(text.substring(i, i + 1)) - 1);
        }
        // spacing s'applique entre les caracteres (N-1 gaps) mais on l'ajoute apres chaque char
        // pour la largeur totale, on ajoute spacing * N (leger sur-estimation acceptable pour centrage)
        return total + (int)(spacing * text.length());
    }

    public static FontRenderer fr() {
        return Minecraft.getInstance().font;
    }
}
