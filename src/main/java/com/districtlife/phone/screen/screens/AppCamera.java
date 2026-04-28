package com.districtlife.phone.screen.screens;

import com.districtlife.phone.camera.CameraHelper;
import com.districtlife.phone.camera.CameraModBridge;
import com.districtlife.phone.camera.CameraModImpl;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.districtlife.phone.util.PhoneFont;

/**
 * App Appareil Photo.
 *
 * Affiche un viseur avec un bouton declencheur.
 *
 * Si Camera Mod est installe :
 *   Clic sur le declencheur → ImageTaker.takeScreenshot(randomUUID) :
 *     - Ferme l'ecran (mc.setScreen(null))
 *     - Masque le HUD (hideGui=true)
 *     - Au prochain RenderTickEvent.END : capture + envoi serveur Camera Mod
 *     → Un item "camera:image" apparait dans l'inventaire du joueur
 *     → Visible dans l'AlbumScreen de la Galerie
 *
 * Si Camera Mod est absent :
 *   Capture differee via CameraHelper (sauvegarde dans screenshots/phone/).
 */
@OnlyIn(Dist.CLIENT)
public class AppCamera extends AbstractPhoneApp {

    private static final int BAR_H     = 16;
    private static final int SHUTTER_R = 14;

    // -------------------------------------------------------------------------
    // Rendu
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        drawTitleBar(stack, mouseX, mouseY);

        int contentY = phoneY + BAR_H;
        int contentH = phoneHeight - BAR_H;
        int cx       = phoneX + phoneWidth / 2;
        int cy       = phoneY + phoneHeight / 2;

        // --- Fond du viseur ---
        PhoneRenderHelper.fillRect(stack, phoneX, contentY, phoneWidth, contentH, 0xFF0A0A12);

        // Lignes de visee
        PhoneRenderHelper.fillRect(stack, phoneX + 8, cy - 1, phoneWidth - 16, 1, 0x33FFFFFF);
        PhoneRenderHelper.fillRect(stack, cx - 1, contentY + 8, 1, contentH - 16, 0x33FFFFFF);

        // --- Etat de la capture en cours (fallback sans Camera Mod) ---
        if (!CameraModBridge.isLoaded() && CameraHelper.isPending()) {
            String waiting = "Capture...";
            int ww = PhoneFont.width(waiting);
            PhoneFont.draw(stack, waiting,
                    phoneX + (phoneWidth - ww) / 2.0F,
                    cy - 4,
                    0xFFFFCC00);
            return;
        }

        // --- Bouton declencheur ---
        int btnY = phoneY + phoneHeight - SHUTTER_R - 10;
        boolean hov = isInCircle(mouseX, mouseY, cx, btnY, SHUTTER_R);

        drawCircle(stack, cx, btnY, SHUTTER_R,     hov ? 0xFFFFFFFF : 0xCCFFFFFF);
        drawCircle(stack, cx, btnY, SHUTTER_R - 3, hov ? 0xFFDDDDDD : 0xAAFFFFFF);
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleBackButtonClick(mx, my)) return true;
        if (button != 0) return false;

        int cx   = phoneX + phoneWidth / 2;
        int btnY = phoneY + phoneHeight - SHUTTER_R - 10;

        if (isInCircle(mx, my, cx, btnY, SHUTTER_R)) {
            if (CameraModBridge.isLoaded() && CameraModImpl.isTriggerAvailable()) {
                // Camera Mod : ferme l'ecran, masque HUD, capture + envoi serveur
                CameraModImpl.triggerCapture();
            } else if (!CameraHelper.isPending()) {
                // Fallback : sauvegarde locale dans screenshots/phone/
                CameraHelper.requestCapture();
            }
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Helpers geometriques
    // -------------------------------------------------------------------------

    private void drawCircle(MatrixStack stack, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt(r * r - dy * dy);
            PhoneRenderHelper.fillRect(stack, cx - half, cy + dy, half * 2, 1, color);
        }
    }

    private boolean isInCircle(double mx, double my, int cx, int cy, int r) {
        double dx = mx - cx;
        double dy = my - cy;
        return dx * dx + dy * dy <= (double) r * r;
    }

    // -------------------------------------------------------------------------
    // Meta
    // -------------------------------------------------------------------------

    @Override
    public String getTitle() {
        return "Appareil Photo";
    }
}
