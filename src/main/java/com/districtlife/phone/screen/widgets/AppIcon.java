package com.districtlife.phone.screen.widgets;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BooleanSupplier;
import com.districtlife.phone.util.PhoneFont;

@OnlyIn(Dist.CLIENT)
public class AppIcon {

    private final int x;
    private final int y;
    private final int size;
    private final String label;
    private final ResourceLocation iconTexture;
    private final AbstractPhoneApp app;
    /** Fournisseur optionnel du point de notification (null = jamais affiche). */
    private final BooleanSupplier notifSupplier;
    private final NotificationDot notificationDot;

    public AppIcon(int x, int y, int size, String label, String iconName, AbstractPhoneApp app) {
        this(x, y, size, label, iconName, app, null);
    }

    private static final ResourceLocation TEX_ICON_BG =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/icon_bg.png");
    private static final ResourceLocation TEX_ICON_BG_HOVER =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/icon_bg_hover.png");

    public AppIcon(int x, int y, int size, String label, String iconName,
                   AbstractPhoneApp app, BooleanSupplier notifSupplier) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.label = label;
        this.iconTexture = new ResourceLocation("districtlife_phone",
                "textures/gui/icons/" + iconName + ".png");
        this.app           = app;
        this.notifSupplier = notifSupplier;
        this.notificationDot = new NotificationDot(x, y, size);
    }

    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);

        // Fond de l'icone
        PhoneRenderHelper.drawTexture(stack, hovered ? TEX_ICON_BG_HOVER : TEX_ICON_BG, x, y, size, size);

        // Texture de l'icone
        PhoneRenderHelper.drawTexture(stack, iconTexture, x + 4, y + 4, size - 8, size - 8);

        // Label sous l'icone
        int labelWidth = PhoneFont.width(label);
        float labelX = x + (size - labelWidth) / 2.0F;
        float labelY = y + size + 2;
        PhoneFont.draw(stack, label, labelX, labelY, 0xFFFFFFFF);

        // Point de notification (rouge, coin superieur droit)
        if (notifSupplier != null) {
            notificationDot.setVisible(notifSupplier.getAsBoolean());
            notificationDot.render(stack);
        }
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }

    public AbstractPhoneApp getApp() {
        return app;
    }
}
