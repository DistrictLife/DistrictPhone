package com.districtlife.phone.screen.screens;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Application carte du telephone.
 *
 * Projection basee sur "doc/ratio carte to map.md" :
 *   - Monde    : 5120 x 5120 blocs (de -2560 a +2560 sur X et Z)
 *   - Image    : 10 000 x 10 000 pixels
 *   - Ratio    : 10000 / 5120 = ~1,953 pixels PNG par bloc Minecraft
 *   - Formule  : pixelX = blocX * PPB + MAP_PIXELS/2
 *                pixelZ = blocZ * PPB + MAP_PIXELS/2
 */
@OnlyIn(Dist.CLIENT)
public class AppMap extends AbstractPhoneApp {

    private static final ResourceLocation MAP_TEXTURE =
            new ResourceLocation("districtlife_phone", "textures/gui/map_zone_serveur.png");

    // -------------------------------------------------------------------------
    // Constantes de projection
    // -------------------------------------------------------------------------

    /** Demi-taille du monde en blocs (monde de -WORLD_HALF a +WORLD_HALF) */
    private static final double WORLD_HALF = 2560.0;
    /** Taille de l'image PNG exportee (carree) */
    private static final int    MAP_PIXELS = 10_000;
    /** Pixels PNG par bloc Minecraft = MAP_PIXELS / (2 * WORLD_HALF) ≈ 1.953 */
    private static final double PPB        = MAP_PIXELS / (2.0 * WORLD_HALF);

    // -------------------------------------------------------------------------
    // Zoom  (pixels ecran par bloc Minecraft)
    // -------------------------------------------------------------------------

    private static final float ZOOM_DEFAULT = 0.30f;   // ~533 blocs visibles en largeur
    private static final float ZOOM_MIN     = 0.04f;   // vue globale ~4000 blocs
    private static final float ZOOM_MAX     = 3.0f;    // vue detail  ~53 blocs

    // -------------------------------------------------------------------------
    // Etat de la vue
    // -------------------------------------------------------------------------

    private float   zoom       = ZOOM_DEFAULT;
    /** Decalage de panoramique en pixels ecran depuis la position du joueur */
    private float   panX       = 0;
    private float   panZ       = 0;

    private boolean isDragging = false;
    private double  dragLastX, dragLastY;

    /** Hauteur de la barre de titre (fixee dans AbstractPhoneApp.drawTitleBar) */
    private static final int BAR_H = 16;

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void onInit() {
        zoom       = ZOOM_DEFAULT;
        panX       = 0;
        panZ       = 0;
        isDragging = false;
    }

    // -------------------------------------------------------------------------
    // Rendu principal
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partial) {
        drawTitleBar(stack, mouseX, mouseY);

        int mapX = phoneX;
        int mapY = phoneY + BAR_H;
        int mapW = phoneWidth;
        int mapH = phoneHeight - BAR_H;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        // Fond sombre (visible lorsque le joueur est hors des limites de la carte)
        PhoneRenderHelper.fillRect(stack, mapX, mapY, mapW, mapH, 0xFF0D1117);

        // --- Calculs de projection ---
        // Position joueur dans l'espace pixels de l'image PNG
        double playerPngX = px * PPB + MAP_PIXELS / 2.0;
        double playerPngZ = pz * PPB + MAP_PIXELS / 2.0;

        // Echelle : pixels ecran par pixel PNG
        double scale = zoom / PPB;

        // Nombre de pixels PNG visibles dans la zone map de l'ecran
        double pngVisW = mapW / scale;
        double pngVisH = mapH / scale;

        // Centre de la vue en coordonnees PNG  (joueur + decalage de panoramique)
        //   panX/scale convertit le decalage ecran en decalage PNG
        double vcPngX = playerPngX - panX / scale;
        double vcPngZ = playerPngZ - panZ / scale;

        // Coin superieur gauche de la region PNG affichee
        double pngMinX = vcPngX - pngVisW / 2.0;
        double pngMinZ = vcPngZ - pngVisH / 2.0;

        // --- Dessin ---
        drawMapTexture(stack, mapX, mapY, mapW, mapH, pngMinX, pngMinZ, pngVisW, pngVisH);
        drawPlayerMarker(stack, mapX, mapY, mapW, mapH,
                playerPngX, playerPngZ, pngMinX, pngMinZ, pngVisW, pngVisH,
                mc.player.yRot);

        if (Math.abs(panX) > 3 || Math.abs(panZ) > 3) {
            drawRecenterButton(stack, mouseX, mouseY, mapX, mapY, mapW);
        }
        drawZoomButtons(stack, mouseX, mouseY, mapX, mapY, mapW, mapH);
        drawScaleBar(stack, mapX, mapY, mapH);
    }

    // -------------------------------------------------------------------------
    // Dessin – texture de la carte
    // -------------------------------------------------------------------------

    /**
     * Dessine la portion visible de la texture map_zone_serveur.png.
     *
     * On utilise AbstractGui.blit(11 args) qui scale une region source (srcW x srcH pixels PNG)
     * vers une region destination (dstW x dstH pixels ecran), exactement ce dont on a besoin.
     */
    private void drawMapTexture(MatrixStack stack,
                                 int mapX, int mapY, int mapW, int mapH,
                                 double pngMinX, double pngMinZ,
                                 double pngVisW, double pngVisH) {
        double pngMaxX = pngMinX + pngVisW;
        double pngMaxZ = pngMinZ + pngVisH;

        // Clampage a la zone valide de la texture [0, MAP_PIXELS]
        double cMinX = Math.max(0, pngMinX);
        double cMinZ = Math.max(0, pngMinZ);
        double cMaxX = Math.min(MAP_PIXELS, pngMaxX);
        double cMaxZ = Math.min(MAP_PIXELS, pngMaxZ);
        if (cMaxX <= cMinX || cMaxZ <= cMinZ) return;

        // Fraction de la zone visible totale occupee par la region clampee
        double fMinX = (cMinX - pngMinX) / pngVisW;
        double fMinZ = (cMinZ - pngMinZ) / pngVisH;
        double fMaxX = (cMaxX - pngMinX) / pngVisW;
        double fMaxZ = (cMaxZ - pngMinZ) / pngVisH;

        // Destination ecran correspondante
        int dstX = mapX + (int)(fMinX * mapW);
        int dstY = mapY + (int)(fMinZ * mapH);
        int dstW = Math.max(1, (int) Math.round((fMaxX - fMinX) * mapW));
        int dstH = Math.max(1, (int) Math.round((fMaxZ - fMinZ) * mapH));

        // Source PNG (entiers pour precision suffisante)
        int srcU = (int) Math.round(cMinX);
        int srcV = (int) Math.round(cMinZ);
        int srcW = Math.max(1, (int) Math.round(cMaxX - cMinX));
        int srcH = Math.max(1, (int) Math.round(cMaxZ - cMinZ));

        RenderSystem.color4f(1f, 1f, 1f, 1f);
        Minecraft.getInstance().getTextureManager().bind(MAP_TEXTURE);
        // blit(stack, dstX, dstY, dstW, dstH, uOffset, vOffset, uWidth, vHeight, texW, texH)
        AbstractGui.blit(stack, dstX, dstY, dstW, dstH,
                (float) srcU, (float) srcV, srcW, srcH, MAP_PIXELS, MAP_PIXELS);
    }

    // -------------------------------------------------------------------------
    // Dessin – marqueur joueur
    // -------------------------------------------------------------------------

    /**
     * Affiche un point rouge avec une petite fleche de direction.
     *
     * Direction depuis le yaw Minecraft :
     *   yRot = 0   -> sud (+Z) -> bas sur la carte
     *   yRot = 90  -> ouest (-X) -> gauche
     *   yRot = -90 -> est (+X) -> droite
     *   yRot = 180 -> nord (-Z) -> haut
     *
     *   adx = -sin(yRot),  adz = cos(yRot)
     */
    private void drawPlayerMarker(MatrixStack stack,
                                   int mapX, int mapY, int mapW, int mapH,
                                   double playerPngX, double playerPngZ,
                                   double pngMinX, double pngMinZ,
                                   double pngVisW, double pngVisH,
                                   float yRot) {
        // Position ecran du joueur
        double fracX = (playerPngX - pngMinX) / pngVisW;
        double fracZ = (playerPngZ - pngMinZ) / pngVisH;
        int sx = mapX + (int)(fracX * mapW);
        int sz = mapY + (int)(fracZ * mapH);

        // Hors de la zone map : ne pas dessiner
        if (sx < mapX || sx > mapX + mapW || sz < mapY || sz > mapY + mapH) return;

        // Vecteur direction (espace ecran)
        float adx = -(float) Math.sin(Math.toRadians(yRot));
        float adz =  (float) Math.cos(Math.toRadians(yRot));

        // Pointe de la fleche (6px devant le point)
        int tipX = sx + (int)(adx * 6);
        int tipZ = sz + (int)(adz * 6);

        // Ombre sous la pointe
        PhoneRenderHelper.fillRect(stack, tipX - 1, tipZ - 1, 4, 4, 0x99000000);
        // Pointe orange
        PhoneRenderHelper.fillRect(stack, tipX, tipZ, 2, 2, 0xFFFF8844);

        // Ombre sous le point principal
        PhoneRenderHelper.fillRect(stack, sx - 3, sz - 3, 7, 7, 0x99000000);
        // Point rouge
        PhoneRenderHelper.fillRect(stack, sx - 2, sz - 2, 5, 5, 0xFFDD2222);
        // Reflet central clair
        PhoneRenderHelper.fillRect(stack, sx - 1, sz - 1, 2, 2, 0xFFFF8888);
    }

    // -------------------------------------------------------------------------
    // Dessin – controles
    // -------------------------------------------------------------------------

    private void drawRecenterButton(MatrixStack stack, int mouseX, int mouseY,
                                     int mapX, int mapY, int mapW) {
        int btnX = mapX + mapW - 18;
        int btnY = mapY + 4;
        boolean hov = isIn(mouseX, mouseY, btnX, btnY, 14, 12);
        PhoneRenderHelper.fillRect(stack, btnX, btnY, 14, 12, hov ? 0xCC224477 : 0x99224477);
        PhoneRenderHelper.drawBorder(stack, btnX, btnY, 14, 12, 1, 0xFF4488CC);
        // Symbole "point de visee" ◎
        getFont().draw(stack, "\u25CE", btnX + 2, btnY + 2, 0xFFCCEEFF);
    }

    private void drawZoomButtons(MatrixStack stack, int mouseX, int mouseY,
                                  int mapX, int mapY, int mapW, int mapH) {
        int btnX = mapX + mapW - 18;
        int btnYIn  = mapY + mapH - 36;
        int btnYOut = btnYIn + 16;

        boolean hovIn  = isIn(mouseX, mouseY, btnX, btnYIn,  14, 14);
        boolean hovOut = isIn(mouseX, mouseY, btnX, btnYOut, 14, 14);

        PhoneRenderHelper.fillRect (stack, btnX, btnYIn,  14, 14, hovIn  ? 0xCC334455 : 0x99334455);
        PhoneRenderHelper.fillRect (stack, btnX, btnYOut, 14, 14, hovOut ? 0xCC334455 : 0x99334455);
        PhoneRenderHelper.drawBorder(stack, btnX, btnYIn,  14, 14, 1, 0xFF446688);
        PhoneRenderHelper.drawBorder(stack, btnX, btnYOut, 14, 14, 1, 0xFF446688);
        getFont().draw(stack, "+", btnX + 4, btnYIn  + 3, 0xFFFFFFFF);
        getFont().draw(stack, "-", btnX + 5, btnYOut + 3, 0xFFFFFFFF);
    }

    // -------------------------------------------------------------------------
    // Dessin – barre d'echelle
    // -------------------------------------------------------------------------

    /**
     * Calcule une distance "propre" (arrondie) representee par 40 pixels ecran,
     * trace la barre correspondante et affiche le label en metres / km.
     */
    private void drawScaleBar(MatrixStack stack, int mapX, int mapY, int mapH) {
        double blocksFor40px = 40.0 / zoom;
        double niceBlocks    = roundToNice(blocksFor40px);
        int    barPx         = Math.max(4, (int) Math.round(niceBlocks * zoom));

        String label = niceBlocks >= 1000
                ? String.format("%.0f km", niceBlocks / 1000.0)
                : String.format("%.0f m",  niceBlocks);

        int barX = mapX + 4;
        int barY = mapY + mapH - 11;
        int textW = getFont().width(label);

        PhoneRenderHelper.fillRect(stack, barX - 1, barY - 2, barPx + 6 + textW, 11, 0x88000000);
        PhoneRenderHelper.fillRect(stack, barX, barY + 4,      barPx, 2, 0xDDFFFFFF);
        PhoneRenderHelper.fillRect(stack, barX, barY + 2,          1, 6, 0xDDFFFFFF);
        PhoneRenderHelper.fillRect(stack, barX + barPx - 1, barY + 2, 1, 6, 0xDDFFFFFF);
        getFont().draw(stack, label, barX + barPx + 3, barY + 1, 0xFFFFFFFF);
    }

    /** Arrondit a une valeur "lisible" pour la legende d'echelle. */
    private static double roundToNice(double value) {
        double[] steps = { 10, 20, 50, 100, 200, 500, 1000, 2000, 5000 };
        for (double s : steps) {
            if (s >= value * 0.75) return s;
        }
        return steps[steps.length - 1];
    }

    // -------------------------------------------------------------------------
    // Interactions souris
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleBackButtonClick(mx, my)) return true;

        int mapX = phoneX;
        int mapY = phoneY + BAR_H;
        int mapW = phoneWidth;
        int mapH = phoneHeight - BAR_H;

        // Bouton recentrer
        int recX = mapX + mapW - 18;
        int recY = mapY + 4;
        if ((Math.abs(panX) > 3 || Math.abs(panZ) > 3) && isIn(mx, my, recX, recY, 14, 12)) {
            panX = 0;
            panZ = 0;
            return true;
        }

        // Bouton zoom +
        int zBtnX  = mapX + mapW - 18;
        int zBtnYIn  = mapY + mapH - 36;
        int zBtnYOut = zBtnYIn + 16;
        // Boutons zoom : centrage sur le milieu de la carte (cx=cz=0)
        if (isIn(mx, my, zBtnX, zBtnYIn,  14, 14)) { zoomAt(1.5f,         mapX + mapW / 2.0, mapY + mapH / 2.0); return true; }
        if (isIn(mx, my, zBtnX, zBtnYOut, 14, 14)) { zoomAt(1.0f / 1.5f, mapX + mapW / 2.0, mapY + mapH / 2.0); return true; }

        // Debut de panoramique (partout sauf les boutons)
        if (isIn(mx, my, mapX, mapY, mapW, mapH)) {
            isDragging = true;
            dragLastX  = mx;
            dragLastY  = my;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (isDragging) {
            panX += (float)(mx - dragLastX);
            panZ += (float)(my - dragLastY);
            dragLastX = mx;
            dragLastY = my;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        isDragging = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        zoomAt(delta > 0 ? 1.20f : 1.0f / 1.20f, mx, my);
        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Zoom vers un point precis de l'ecran (typiquement le curseur).
     *
     * Le pixel de carte sous (cursorScreenX, cursorScreenY) reste fixe apres le zoom.
     * Derivation : si cx = cursorX - mapCenterX, le nouveau pan verifie :
     *   panX_new = cx * (1 - actualFactor) + panX * actualFactor
     * Ce qui garantit que vcPngX + cx/scale est invariant.
     */
    private void zoomAt(float factor, double cursorScreenX, double cursorScreenY) {
        float newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * factor));
        float actualFactor = newZoom / zoom;  // facteur reel apres clampage

        // Offset du curseur par rapport au centre de la zone map
        float cx = (float)(cursorScreenX - (phoneX + phoneWidth  / 2.0));
        float cz = (float)(cursorScreenY - (phoneY + BAR_H + (phoneHeight - BAR_H) / 2.0));

        panX = cx * (1f - actualFactor) + panX * actualFactor;
        panZ = cz * (1f - actualFactor) + panZ * actualFactor;
        zoom = newZoom;
    }

    private boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public String getTitle() { return "Map"; }
}
