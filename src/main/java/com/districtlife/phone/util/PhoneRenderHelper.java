package com.districtlife.phone.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class PhoneRenderHelper {

    /**
     * Dessine un rectangle plein avec la couleur ARGB specifiee.
     * Utilise la methode vanilla fill().
     */
    public static void fillRect(MatrixStack stack, int x, int y, int width, int height, int color) {
        AbstractGui.fill(stack, x, y, x + width, y + height, color);
    }

    /**
     * Dessine un rectangle avec un contour (border) colore, fond transparent.
     */
    public static void drawBorder(MatrixStack stack, int x, int y, int width, int height,
                                   int borderThickness, int color) {
        fillRect(stack, x, y, width, borderThickness, color);                           // haut
        fillRect(stack, x, y + height - borderThickness, width, borderThickness, color); // bas
        fillRect(stack, x, y, borderThickness, height, color);                           // gauche
        fillRect(stack, x + width - borderThickness, y, borderThickness, height, color); // droite
    }

    /**
     * Lie une texture et la dessine dans le rectangle specifie (region 0,0 -> texWxtexH).
     */
    public static void drawTexture(MatrixStack stack, ResourceLocation texture,
                                    int x, int y, int width, int height,
                                    int texWidth, int texHeight) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bind(texture);
        AbstractGui.blit(stack, x, y, 0, 0, width, height, texWidth, texHeight);
    }

    /**
     * Dessine une texture en taille naturelle (texWidth = width, texHeight = height).
     */
    public static void drawTexture(MatrixStack stack, ResourceLocation texture,
                                    int x, int y, int width, int height) {
        drawTexture(stack, texture, x, y, width, height, width, height);
    }

    /**
     * Dessine un fond semi-transparent (overlay sombre) sur tout l'ecran.
     */
    public static void drawDimBackground(MatrixStack stack, int screenWidth, int screenHeight) {
        fillRect(stack, 0, 0, screenWidth, screenHeight, 0xB0000000);
    }

    /**
     * Dessine du texte centre horizontalement dans une zone donnee.
     */
    public static void drawCenteredText(MatrixStack stack, net.minecraft.client.gui.FontRenderer font,
                                         String text, int x, int y, int maxWidth, int color) {
        int textWidth = font.width(text);
        int drawX = x + (maxWidth - textWidth) / 2;
        font.draw(stack, text, drawX, y, color);
    }

    /**
     * Dessine une texture en la redimensionnant dans le rectangle cible.
     * srcW x srcH = taille reelle de la texture ; dstW x dstH = taille d'affichage.
     */
    public static void drawTextureScaled(MatrixStack stack, ResourceLocation texture,
                                          int x, int y, int dstW, int dstH,
                                          int srcW, int srcH) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bind(texture);
        AbstractGui.blit(stack, x, y, dstW, dstH, 0f, 0f, srcW, srcH, srcW, srcH);
    }
}
