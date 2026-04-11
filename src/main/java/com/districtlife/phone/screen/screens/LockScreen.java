package com.districtlife.phone.screen.screens;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.districtlife.phone.util.RPTime;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LockScreen extends AbstractPhoneApp {

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        // Fond semi-transparent par-dessus le wallpaper
        PhoneRenderHelper.fillRect(stack, phoneX, phoneY, phoneWidth, phoneHeight, 0x55000000);

        long dayTime = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getDayTime() : 0L;
        RPTime rpTime = new RPTime(dayTime);

        // Heure en grand (centre)
        String timeStr = rpTime.getFormattedTime();
        net.minecraft.client.gui.FontRenderer font = getFont();

        // Scale x2 pour l'heure - on simule en dessinant deux fois decale (pas de scale Matrix ici)
        // Pour un vrai scale, utiliser MatrixStack.scale + translate
        stack.pushPose();
        stack.translate(phoneX + phoneWidth / 2.0, phoneY + phoneHeight / 2.0 - 50, 0);
        stack.scale(2.5F, 2.5F, 1.0F);
        int timeWidth = font.width(timeStr);
        font.draw(stack, timeStr, -timeWidth / 2.0F, -8, 0xFFFFFFFF);
        stack.popPose();

        // Date RP
        String dateStr = rpTime.getFormattedDate();
        int dateWidth = font.width(dateStr);
        font.draw(stack, dateStr,
                phoneX + (phoneWidth - dateWidth) / 2.0F,
                phoneY + phoneHeight / 2.0F - 15,
                0xFFCCCCCC);

        // Instruction deverrouillage (bas)
        String unlockStr = "Appuyer pour deverrouiller";
        int unlockWidth = font.width(unlockStr);
        font.draw(stack, unlockStr,
                phoneX + (phoneWidth - unlockWidth) / 2.0F,
                phoneY + phoneHeight - 30,
                0xFFAAAAAA);
    }

    @Override
    public String getTitle() {
        return "Verrouille";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Tout clic deverrouille
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
