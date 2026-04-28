package com.districtlife.phone.screen.screens;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketSetAppHidden;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.districtlife.phone.util.PhoneFont;

/**
 * App Parametres.
 *
 * Section 1 : Numero de telephone (affichage seul).
 * Section 2 : Applications — toggle masquer/afficher chaque app (sauf Parametres).
 *             Envoie PacketSetAppHidden au serveur, synce via PacketSyncPhone.
 */
@OnlyIn(Dist.CLIENT)
public class AppSettings extends AbstractPhoneApp {

    private static final int BAR_H    = 16;
    private static final int ROW_H    = 16; // hauteur de chaque ligne d'app
    private static final int PADDING  = 8;

    // Scroll pour la section Applications
    private int scrollOffset = 0;

    // Zone Y de la section Applications (calculee au rendu)
    private int appsContentY = 0;
    private int appsContentH = 0;

    @Override
    protected void onInit() {
        scrollOffset = 0;
    }

    // -------------------------------------------------------------------------
    // Rendu
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        drawTitleBar(stack, mouseX, mouseY);

        int y = phoneY + BAR_H + PADDING;

        // ── Section numero de telephone ─────────────────────────────────────
        y = renderSectionHeader(stack, "Numero de telephone", y);

        String number  = phoneScreen.getPhoneNumber();
        String display = number.isEmpty() ? "Non attribue" : number;

        stack.pushPose();
        float scale = 1.3F;
        float nw = PhoneFont.width(display) * scale;
        stack.translate(phoneX + (phoneWidth - nw) / 2.0F, y, 0);
        stack.scale(scale, scale, 1.0F);
        PhoneFont.draw(stack, display, 0, 0, 0xFFFFFFFF);
        stack.popPose();

        y += (int) (9 * scale) + PADDING + 4;

        // ── Section Applications ─────────────────────────────────────────────
        y = renderSectionHeader(stack, "Applications", y);

        ItemStack phoneStack = getPhoneStack();
        int availableH = phoneY + phoneHeight - y - 2;
        appsContentY = y;
        appsContentH = availableH;

        // Total height of the apps list (only non-settings apps)
        int appCount = countToggleableApps();
        int totalH   = appCount * ROW_H;

        int maxScroll = Math.max(0, totalH - availableH);
        scrollOffset  = Math.min(scrollOffset, maxScroll);

        enableScissor(phoneX, y, phoneWidth, availableH);

        int rowY = y - scrollOffset;
        for (Object[] def : HomeScreen.ALL_APPS) {
            String appId = (String) def[1];
            if ("app_settings".equals(appId)) continue; // jamais masquable

            String label  = (String) def[0];
            boolean hidden = PhoneData.isAppHidden(phoneStack, appId);

            boolean hovRow = mouseX >= phoneX && mouseX <= phoneX + phoneWidth
                          && mouseY >= rowY   && mouseY <= rowY + ROW_H;

            // Fond au survol
            if (hovRow) {
                PhoneRenderHelper.fillRect(stack, phoneX, rowY, phoneWidth, ROW_H, 0x22FFFFFF);
            }

            // Bullet colore : vert = visible, gris = masque
            int bulletColor = hidden ? 0xFF666666 : 0xFF44BB44;
            PhoneFont.draw(stack, "\u25CF", phoneX + PADDING, rowY + 4, bulletColor);

            // Nom de l'app
            PhoneFont.draw(stack, label, phoneX + PADDING + 10, rowY + 4, 0xFFDDDDDD);

            // Badge MASQUE ou (rien si visible)
            if (hidden) {
                String badge = "MASQUE";
                int bw = PhoneFont.width(badge);
                PhoneFont.draw(stack, badge,
                        phoneX + phoneWidth - bw - PADDING,
                        rowY + 4,
                        0xFF888888);
            }

            rowY += ROW_H;
        }

        disableScissor();

        // Scrollbar si necessaire
        if (maxScroll > 0) {
            int barH = Math.max(8, availableH * availableH / totalH);
            int barY = appsContentY + scrollOffset * (availableH - barH) / maxScroll;
            PhoneRenderHelper.fillRect(stack, phoneX + phoneWidth - 3, barY, 2, barH, 0x66FFFFFF);
        }
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleBackButtonClick(mx, my)) return true;
        if (button != 0) return false;

        // Clic dans la zone des apps
        if (my < appsContentY || my > appsContentY + appsContentH) return false;
        if (mx < phoneX || mx > phoneX + phoneWidth) return false;

        ItemStack phoneStack = getPhoneStack();
        int rowY = appsContentY - scrollOffset;

        for (Object[] def : HomeScreen.ALL_APPS) {
            String appId = (String) def[1];
            if ("app_settings".equals(appId)) continue;

            if (my >= rowY && my < rowY + ROW_H) {
                boolean wasHidden = PhoneData.isAppHidden(phoneStack, appId);
                boolean nowHidden = !wasHidden;

                // Mise a jour optimiste locale (retour visuel immediat)
                PhoneData.setAppHidden(phoneStack, appId, nowHidden);

                // Envoi au serveur pour persistance
                PacketHandler.CHANNEL.sendToServer(
                        new PacketSetAppHidden(
                                phoneScreen.getPhoneNumber(), appId, nowHidden));
                return true;
            }
            rowY += ROW_H;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (my >= appsContentY && my <= appsContentY + appsContentH) {
            scrollOffset = Math.max(0, scrollOffset - (int) (delta * 10));
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int renderSectionHeader(MatrixStack stack, String title, int y) {
        int tw = PhoneFont.width(title);
        PhoneFont.draw(stack, title,
                phoneX + (phoneWidth - tw) / 2.0F, y, 0xFF888888);
        y += 10;
        PhoneRenderHelper.fillRect(stack, phoneX + 6, y, phoneWidth - 12, 1, 0xFF333355);
        return y + 5;
    }

    private int countToggleableApps() {
        int count = 0;
        for (Object[] def : HomeScreen.ALL_APPS) {
            if (!"app_settings".equals(def[1])) count++;
        }
        return count;
    }

    private void enableScissor(int x, int y, int w, int h) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int sx = (int)(x * scale);
        int sy = (int)((mc.getWindow().getGuiScaledHeight() - y - h) * scale);
        int sw = (int)(w * scale);
        int sh = (int)(h * scale);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    private void disableScissor() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }

    // -------------------------------------------------------------------------
    // Meta
    // -------------------------------------------------------------------------

    @Override
    public String getTitle() {
        return "Parametres";
    }
}
