package com.districtlife.phone.screen.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class DebugTextureScreen extends Screen {

    private final String rawInput;
    private final ResourceLocation location;

    public DebugTextureScreen(String texturePath) {
        super(new StringTextComponent("Debug Texture"));
        this.rawInput = texturePath;

        // Normalise le chemin : ajoute "textures/" si absent, ".png" si absent
        String path = texturePath;
        if (!path.startsWith("textures/")) path = "textures/" + path;
        if (!path.endsWith(".png"))        path = path + ".png";

        this.location = new ResourceLocation("districtlife_phone", path);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        // Fond sombre
        renderBackground(stack);

        int texW = 0;
        int texH = 0;
        boolean loaded = false;

        try {
            // Bind la texture — Minecraft charge le fichier si besoin
            RenderSystem.color4f(1f, 1f, 1f, 1f);
            minecraft.getTextureManager().bind(location);
            // Lit les dimensions depuis l'etat OpenGL courant (la texture vient d'etre bindee)
            texW = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            texH = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            loaded = texW > 0 && texH > 0;
        } catch (Exception ignored) {}

        int headerH = 28;
        int footerH = 14;

        if (loaded) {
            // Calcule l'echelle pour tenir dans la zone disponible
            int maxW = width - 20;
            int maxH = height - headerH - footerH - 10;
            float scale = Math.min((float) maxW / texW, (float) maxH / texH);
            int drawW  = (int) (texW * scale);
            int drawH  = (int) (texH * scale);
            int drawX  = (width  - drawW) / 2;
            int drawY  = headerH + (maxH - drawH) / 2;

            // Texture (deja bindee depuis la lecture des dimensions)
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            minecraft.getTextureManager().bind(location);
            blit(stack, drawX, drawY, drawW, drawH, 0f, 0f, texW, texH, texW, texH);
            RenderSystem.disableBlend();

            // Bordure blanche autour de la texture
            net.minecraft.client.gui.AbstractGui.fill(stack,
                    drawX - 1, drawY - 1, drawX + drawW + 1, drawY, 0xFFFFFFFF);
            net.minecraft.client.gui.AbstractGui.fill(stack,
                    drawX - 1, drawY + drawH, drawX + drawW + 1, drawY + drawH + 1, 0xFFFFFFFF);
            net.minecraft.client.gui.AbstractGui.fill(stack,
                    drawX - 1, drawY, drawX, drawY + drawH, 0xFFFFFFFF);
            net.minecraft.client.gui.AbstractGui.fill(stack,
                    drawX + drawW, drawY, drawX + drawW + 1, drawY + drawH, 0xFFFFFFFF);

            // Infos
            String info = texW + " x " + texH + " px  |  affiche : " + drawW + " x " + drawH;
            drawCenteredString(stack, font, info, width / 2, 17, 0x888888);
        } else {
            // Texture introuvable
            drawCenteredString(stack, font,
                    "§cTexture introuvable", width / 2, height / 2 - 8, 0xFFFFFF);
            drawCenteredString(stack, font,
                    location.toString(), width / 2, height / 2 + 4, 0x666666);
        }

        // En-tete
        drawCenteredString(stack, font,
                "§e[DEBUG]§r  " + rawInput, width / 2, 6, 0xFFFFFF);

        // Pied de page
        drawCenteredString(stack, font,
                "§7[Echap] Fermer", width / 2, height - footerH, 0xFFFFFF);

        super.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
