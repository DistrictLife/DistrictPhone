package com.districtlife.phone.screen.screens;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AppMap extends AbstractPhoneApp {

    private static final ResourceLocation MAP_TEXTURE =
            new ResourceLocation("districtlife_phone", "textures/gui/map_island.png");

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        int barHeight = drawTitleBar(stack, mouseX, mouseY);

        // Affiche la carte statique de l'island
        int mapY = phoneY + barHeight;
        int mapHeight = phoneHeight - barHeight;
        PhoneRenderHelper.drawTexture(stack, MAP_TEXTURE,
                phoneX, mapY, phoneWidth, mapHeight);
    }

    @Override
    public String getTitle() {
        return "Map";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return handleBackButtonClick(mouseX, mouseY);
    }
}
