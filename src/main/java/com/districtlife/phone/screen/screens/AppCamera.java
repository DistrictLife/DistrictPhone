package com.districtlife.phone.screen.screens;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AppCamera extends AbstractPhoneApp {

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        drawTitleBar(stack, mouseX, mouseY);
        drawPlaceholder(stack);
    }

    @Override
    public String getTitle() {
        return "Appareil Photo";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return handleBackButtonClick(mouseX, mouseY);
    }
}
