package com.districtlife.phone.screen.widgets;

import com.districtlife.phone.util.PhoneRenderHelper;
import com.districtlife.phone.util.RPTime;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StatusBar {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public StatusBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        // Fond de la barre
        PhoneRenderHelper.fillRect(stack, x, y, width, height, 0xCC000000);

        FontRenderer font = Minecraft.getInstance().font;

        // Heure RP compacte a gauche
        long dayTime = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getDayTime() : 0L;
        RPTime rpTime = new RPTime(dayTime);
        font.draw(stack, rpTime.getFormattedTime(), x + 4, y + 3, 0xFFFFFFFF);

        // Indicateurs decoratifs a droite : signal + batterie (texte)
        String indicators = "\u25A0\u25A0\u25A0 \uD83D\uDD0B";
        String signalBat = "|||  100%";
        int indWidth = font.width(signalBat);
        font.draw(stack, signalBat, x + width - indWidth - 4, y + 3, 0xFF88FF88);
    }
}
