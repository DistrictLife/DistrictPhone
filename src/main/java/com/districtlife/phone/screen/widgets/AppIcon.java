package com.districtlife.phone.screen.widgets;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AppIcon {

    private static final String ICON_PATH_PREFIX = "districtlife_phone:textures/gui/icons/";

    private final int x;
    private final int y;
    private final int size;
    private final String label;
    private final ResourceLocation iconTexture;
    private final AbstractPhoneApp app;

    public AppIcon(int x, int y, int size, String label, String iconName, AbstractPhoneApp app) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.label = label;
        this.iconTexture = new ResourceLocation("districtlife_phone",
                "textures/gui/icons/" + iconName + ".png");
        this.app = app;
    }

    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);

        // Fond de l'icone (arrondi simule avec un rect, legerement eclairci si hover)
        int bgColor = hovered ? 0x88FFFFFF : 0x55FFFFFF;
        PhoneRenderHelper.fillRect(stack, x, y, size, size, bgColor);

        // Texture de l'icone
        PhoneRenderHelper.drawTexture(stack, iconTexture, x + 4, y + 4, size - 8, size - 8);

        // Label sous l'icone (scale 0.6 pour reduire la taille du texte)
        FontRenderer font = Minecraft.getInstance().font;
        float scale = 0.6F;
        float labelWidth = font.width(label) * scale;
        float labelX = x + (size - labelWidth) / 2.0F;
        float labelY = y + size + 2;
        stack.pushPose();
        stack.translate(labelX, labelY, 0);
        stack.scale(scale, scale, 1.0F);
        font.draw(stack, label, 0, 0, 0xFFFFFFFF);
        stack.popPose();
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }

    public AbstractPhoneApp getApp() {
        return app;
    }
}
