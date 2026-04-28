package com.districtlife.phone.screen.screens;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.screen.widgets.StatusBar;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.districtlife.phone.util.RPTime;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.districtlife.phone.util.PhoneFont;

@OnlyIn(Dist.CLIENT)
public class LockScreen extends AbstractPhoneApp {

    private static final ResourceLocation TEX_LOCKSCREEN_OVERLAY =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/lockscreen_overlay.png");
    private static final ResourceLocation TEX_LOCK_BAR =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/deverouillage/screendown/screen_lock_bar.png");

    // Dimensions originales a 1:1 (424x900 content)
    private static final int LOCK_BAR_W_REF = 154;
    private static final int LOCK_BAR_H_REF = 5;
    private static final int STATUS_BAR_H_REF = 20;
    private static final int LOCK_BAR_MARGIN_REF = 30;

    private StatusBar statusBar;

    @Override
    public void onInit() {
        statusBar = new StatusBar(phoneX, phoneY, phoneWidth, phoneHeight);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        PhoneRenderHelper.drawTexture(stack, TEX_LOCKSCREEN_OVERLAY, phoneX, phoneY, phoneWidth, phoneHeight);

        // Status bar en haut
        if (statusBar != null) statusBar.render(stack, mouseX, mouseY, partialTicks);

        long dayTime = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getDayTime() : 0L;
        RPTime rpTime = new RPTime(dayTime);

        int centerX = phoneX + phoneWidth / 2;
        // Heure a ~20% du haut
        int groupY = phoneY + phoneHeight / 5;

        // Heure : scale limite par la largeur (85%) ET la hauteur (25% du phone)
        // Police Minecraft = 8px natif => scale = targetH / 8
        String timeStr = rpTime.getFormattedTime();
        int timeRawW = PhoneFont.widthNoGap(timeStr);
        float maxScaleByHeight = phoneHeight * 0.11f / 8.0f;
        float maxScaleByWidth  = phoneWidth  * 0.75f / Math.max(1, timeRawW);
        float timeScale = Math.min(maxScaleByHeight, maxScaleByWidth);

        final float CHAR_SPACING = 1.0f; // px supplementaires entre chaque caractere

        stack.pushPose();
        stack.translate(centerX, groupY, 0);
        stack.scale(timeScale, timeScale, 1.0F);
        int timeRawWS = PhoneFont.widthNoGap(timeStr, CHAR_SPACING);
        PhoneFont.drawNoGap(stack, timeStr, -timeRawWS / 2.0F, 0, CHAR_SPACING, 0xFFFFFFFF);
        stack.popPose();

        // Date : 1/3 de la taille de l'heure
        String dateStr = rpTime.getFormattedDate();
        float dateScale = timeScale / 3.0f * 0.85f * 0.70f;
        int dateRenderedH = (int)(8 * dateScale);
        int dateY = groupY - dateRenderedH - 4;

        stack.pushPose();
        stack.translate(centerX, dateY, 0);
        stack.scale(dateScale, dateScale, 1.0F);
        int dateRawWS = PhoneFont.widthNoGap(dateStr, CHAR_SPACING);
        PhoneFont.drawNoGap(stack, dateStr, -dateRawWS / 2.0F, 0, CHAR_SPACING, 0xFFCCCCCC);
        stack.popPose();

        // Barre de deverouillage en bas — dimensionnee comme background/cadre
        float s = phoneWidth / 424.0f;
        int barW      = Math.max(1, (int)(LOCK_BAR_W_REF * s));
        int barH      = Math.max(1, (int)(LOCK_BAR_H_REF * s));
        int barMargin = Math.max(1, (int)(LOCK_BAR_MARGIN_REF * phoneHeight / 900.0f));
        int barX = phoneX + (phoneWidth - barW) / 2;
        int barY = phoneY + phoneHeight - barH - barMargin;
        PhoneRenderHelper.drawTexture(stack, TEX_LOCK_BAR, barX, barY, barW, barH);
    }

    @Override
    public String getTitle() { return ""; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= phoneX && mouseX <= phoneX + phoneWidth
                && mouseY >= phoneY && mouseY <= phoneY + phoneHeight) {
            phoneScreen.navigateTo(phoneScreen.getHomeScreen());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        phoneScreen.navigateTo(phoneScreen.getHomeScreen());
        return true;
    }
}
