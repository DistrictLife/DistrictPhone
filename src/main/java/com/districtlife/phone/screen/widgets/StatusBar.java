package com.districtlife.phone.screen.widgets;

import com.districtlife.phone.util.PhoneFont;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.districtlife.phone.util.RPTime;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StatusBar {

    private static final ResourceLocation TEX_SIGNAL =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/deverouillage/screentop/signal.png");
    private static final ResourceLocation TEX_BATTERY =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/deverouillage/screentop/batterie.png");

    // Dimensions de reference a 1:1 (content 424x900)
    private static final int SIGNAL_W_REF  = 24;
    private static final int SIGNAL_H_REF  = 15;
    private static final int BATTERY_W_REF = 31;
    private static final int BATTERY_H_REF = 15;

    // Positions de reference : batterie a 53px du right, icons a 45px du top, gap 21px
    private static final int BAT_X_REF    = 53;
    private static final int ICON_Y_REF   = 45;
    private static final int ICON_GAP_REF = 21;

    private final int phoneX;
    private final int phoneY;
    private final int phoneWidth;
    private final int phoneHeight;
    private final boolean showTime;

    public StatusBar(int phoneX, int phoneY, int phoneWidth, int phoneHeight) {
        this(phoneX, phoneY, phoneWidth, phoneHeight, false);
    }

    public StatusBar(int phoneX, int phoneY, int phoneWidth, int phoneHeight, boolean showTime) {
        this.phoneX      = phoneX;
        this.phoneY      = phoneY;
        this.phoneWidth  = phoneWidth;
        this.phoneHeight = phoneHeight;
        this.showTime    = showTime;
    }

    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        float s   = phoneWidth / 424.0f;
        int sigW  = Math.max(1, (int)(SIGNAL_W_REF  * s));
        int sigH  = Math.max(1, (int)(SIGNAL_H_REF  * s));
        int batW  = Math.max(1, (int)(BATTERY_W_REF * s));
        int batH  = Math.max(1, (int)(BATTERY_H_REF * s));

        int iconY = phoneY + (int)(ICON_Y_REF * s);
        int batX  = phoneX + phoneWidth - (int)(BAT_X_REF * s) - batW;
        int sigX  = batX - (int)(ICON_GAP_REF * s) - sigW;

        PhoneRenderHelper.drawTexture(stack, TEX_BATTERY, batX, iconY, batW, batH);
        PhoneRenderHelper.drawTexture(stack, TEX_SIGNAL,  sigX, iconY, sigW, sigH);

        if (showTime) {
            long dayTime = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getDayTime() : 0L;
            String timeStr = new RPTime(dayTime).getFormattedTime();
            int timeRawW  = PhoneFont.widthNoGap(timeStr);
            float maxScaleByH = phoneHeight * 0.018f / 8.0f;
            float maxScaleByW = phoneWidth  * 0.12f  / Math.max(1, timeRawW);
            float timeScale   = Math.min(maxScaleByH, maxScaleByW) * 1.03f;
            float textX = phoneX + (int)(61 * s);
            float textY = iconY + (batH - 8 * timeScale) / 2.0f;
            stack.pushPose();
            stack.translate(textX, textY, 0);
            stack.scale(timeScale, timeScale, 1.0f);
            PhoneFont.drawNoGap(stack, timeStr, 0, 0, 0xFFFFFFFF);
            stack.popPose();
        }
    }
}
