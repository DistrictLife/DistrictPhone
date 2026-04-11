package com.districtlife.phone.screen.screens;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AppSettings extends AbstractPhoneApp {

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        int barHeight = drawTitleBar(stack, mouseX, mouseY);

        int contentY = phoneY + barHeight + 12;
        int centerX  = phoneX + phoneWidth / 2;

        // --- Section numero de telephone ---
        String labelSection = "Numero de telephone";
        int lw = getFont().width(labelSection);
        getFont().draw(stack, labelSection,
                phoneX + (phoneWidth - lw) / 2.0F,
                contentY,
                0xFF888888);

        // Separateur
        PhoneRenderHelper.fillRect(stack,
                phoneX + 10, contentY + 11,
                phoneWidth - 20, 1,
                0xFF333355);

        // Numero en grand, centre
        String number = phoneScreen.getPhoneNumber();
        String displayNumber = number.isEmpty() ? "Non attribue" : number;

        stack.pushPose();
        float scale = 1.3F;
        float nw = getFont().width(displayNumber) * scale;
        stack.translate(phoneX + (phoneWidth - nw) / 2.0F, contentY + 18, 0);
        stack.scale(scale, scale, 1.0F);
        getFont().draw(stack, displayNumber, 0, 0, 0xFFFFFFFF);
        stack.popPose();
    }

    @Override
    public String getTitle() {
        return "Parametres";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return handleBackButtonClick(mouseX, mouseY);
    }
}
