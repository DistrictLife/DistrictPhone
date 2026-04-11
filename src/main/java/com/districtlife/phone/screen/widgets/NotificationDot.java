package com.districtlife.phone.screen.widgets;

import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Petit point rouge affiche sur une icone d'app pour signaler un SMS non lu.
 */
@OnlyIn(Dist.CLIENT)
public class NotificationDot {

    private static final int DOT_SIZE = 6;
    private static final int COLOR_RED = 0xFFFF3333;

    private final int x;
    private final int y;
    private boolean visible;

    public NotificationDot(int iconX, int iconY, int iconSize) {
        // Positionne en haut a droite de l'icone
        this.x = iconX + iconSize - DOT_SIZE;
        this.y = iconY;
        this.visible = false;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void render(MatrixStack stack) {
        if (!visible) return;
        PhoneRenderHelper.fillRect(stack, x, y, DOT_SIZE, DOT_SIZE, COLOR_RED);
    }
}
