package com.districtlife.phone.screen.screens;

import com.districtlife.phone.camera.CameraHelper;
import com.districtlife.phone.camera.CameraModBridge;
import com.districtlife.phone.camera.CameraModImpl;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * App Galerie Photo.
 *
 * Si Camera Mod est installe :
 *   Redirige vers AlbumScreen de Camera Mod, alimente par les items "camera:image"
 *   presents dans l'inventaire du joueur.
 *
 * Sinon :
 *   Galerie locale : affiche les photos de screenshots/phone/ (triees du plus recent).
 *   Vue grille (2 colonnes) + vue plein ecran avec navigation.
 */
@OnlyIn(Dist.CLIENT)
public class AppGallery extends AbstractPhoneApp {

    private static final int BAR_H   = 16;
    private static final int PADDING = 4;

    // -------------------------------------------------------------------------
    // Redirect Camera Mod
    // -------------------------------------------------------------------------

    private boolean redirectPending = false;

    // -------------------------------------------------------------------------
    // Etat vue locale
    // -------------------------------------------------------------------------

    private enum View { GRID, FULLSCREEN }

    private View view          = View.GRID;
    private int  fullscreenIdx = 0;
    private int  scrollOffset  = 0;

    private List<String> photoFiles = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Cache de textures : filename -> ResourceLocation
    // -------------------------------------------------------------------------

    private final Map<String, ResourceLocation> textureCache = new LinkedHashMap<>();
    private final Map<String, int[]>            textureDims  = new LinkedHashMap<>();

    private static int texCounter = 0;

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    @Override
    protected void onInit() {
        if (CameraModBridge.isLoaded()) {
            redirectPending = true;
        } else {
            view         = View.GRID;
            scrollOffset = 0;
            scanPhotos();
        }
    }

    @Override
    public void tick() {
        if (redirectPending) {
            redirectPending = false;
            CameraModImpl.openAlbumScreen();
        }
    }

    private void scanPhotos() {
        photoFiles.clear();
        File folder = new File(Minecraft.getInstance().gameDirectory, CameraHelper.PHOTO_FOLDER);
        if (!folder.exists() || !folder.isDirectory()) return;
        File[] files = folder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".png"));
        if (files == null) return;
        Arrays.sort(files, (a, b) -> b.getName().compareTo(a.getName())); // plus recent en premier
        for (File f : files) photoFiles.add(f.getName());
    }

    @Override
    public void onBack() {
        if (view == View.FULLSCREEN) {
            view = View.GRID;
            return;
        }
        releaseTextures();
        super.onBack();
    }

    private void releaseTextures() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation rl : textureCache.values()) {
            if (rl != null) mc.getTextureManager().release(rl);
        }
        textureCache.clear();
        textureDims.clear();
    }

    // -------------------------------------------------------------------------
    // Rendu
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (CameraModBridge.isLoaded()) return; // redirection au prochain tick

        drawTitleBar(stack, mouseX, mouseY);

        if (photoFiles.isEmpty()) {
            drawEmpty(stack);
            return;
        }

        if (view == View.FULLSCREEN) {
            renderFullscreen(stack, mouseX, mouseY);
        } else {
            renderGrid(stack, mouseX, mouseY);
        }
    }

    // -------------------------------------------------------------------------
    // Vue grille
    // -------------------------------------------------------------------------

    private void renderGrid(MatrixStack stack, int mouseX, int mouseY) {
        int contentY = phoneY + BAR_H;
        int contentH = phoneHeight - BAR_H;

        PhoneRenderHelper.fillRect(stack, phoneX, contentY, phoneWidth, contentH, 0xFF111111);

        int cols   = 2;
        int thumbW = (phoneWidth - (cols + 1) * PADDING) / cols;
        int thumbH = thumbW;

        int totalRows = (photoFiles.size() + cols - 1) / cols;
        int totalH    = totalRows * (thumbH + PADDING) + PADDING;
        int maxScroll = Math.max(0, totalH - contentH);
        scrollOffset  = Math.min(scrollOffset, maxScroll);

        enableScissor(phoneX, contentY, phoneWidth, contentH);

        for (int i = 0; i < photoFiles.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int tx  = phoneX + PADDING + col * (thumbW + PADDING);
            int ty  = contentY + PADDING + row * (thumbH + PADDING) - scrollOffset;

            if (ty + thumbH < contentY || ty > contentY + contentH) continue;

            String filename = photoFiles.get(i);
            ResourceLocation rl = getOrLoadTexture(filename);

            if (rl != null) {
                int[] dims = textureDims.get(filename);
                if (dims != null) {
                    PhoneRenderHelper.drawTextureScaled(stack, rl, tx, ty, thumbW, thumbH, dims[0], dims[1]);
                } else {
                    PhoneRenderHelper.drawTexture(stack, rl, tx, ty, thumbW, thumbH);
                }
            } else {
                PhoneRenderHelper.fillRect(stack, tx, ty, thumbW, thumbH, 0xFF333333);
                PhoneRenderHelper.drawCenteredText(stack, getFont(), "?",
                        tx, ty + thumbH / 2 - 4, thumbW, 0xFF888888);
            }

            PhoneRenderHelper.drawBorder(stack, tx, ty, thumbW, thumbH, 1, 0xFF444444);
        }

        disableScissor();

        if (maxScroll > 0) {
            int barH = Math.max(8, contentH * contentH / totalH);
            int barY = contentY + scrollOffset * (contentH - barH) / maxScroll;
            PhoneRenderHelper.fillRect(stack, phoneX + phoneWidth - 3, barY, 2, barH, 0x88FFFFFF);
        }
    }

    // -------------------------------------------------------------------------
    // Vue plein ecran
    // -------------------------------------------------------------------------

    private void renderFullscreen(MatrixStack stack, int mouseX, int mouseY) {
        int contentY = phoneY + BAR_H;
        int contentH = phoneHeight - BAR_H;

        PhoneRenderHelper.fillRect(stack, phoneX, contentY, phoneWidth, contentH, 0xFF050505);

        if (fullscreenIdx < 0) fullscreenIdx = 0;
        if (fullscreenIdx >= photoFiles.size()) fullscreenIdx = photoFiles.size() - 1;

        String filename = photoFiles.get(fullscreenIdx);
        ResourceLocation rl = getOrLoadTexture(filename);

        int arrowY = phoneY + phoneHeight - 12;

        if (rl != null) {
            int[] dims = textureDims.get(filename);
            int dstW = phoneWidth - 2 * PADDING;
            int dstH = contentH  - 2 * PADDING - 14;

            if (dims != null) {
                float ratio = (float) dims[0] / dims[1];
                if (ratio > (float) dstW / dstH) {
                    dstH = (int) (dstW / ratio);
                } else {
                    dstW = (int) (dstH * ratio);
                }
            }

            int imgX = phoneX + (phoneWidth - dstW) / 2;
            int imgY = contentY + PADDING;

            if (dims != null) {
                PhoneRenderHelper.drawTextureScaled(stack, rl, imgX, imgY, dstW, dstH, dims[0], dims[1]);
            } else {
                PhoneRenderHelper.drawTexture(stack, rl, imgX, imgY, dstW, dstH);
            }
        } else {
            PhoneRenderHelper.drawCenteredText(stack, getFont(), "Photo introuvable",
                    phoneX, contentY + contentH / 2 - 4, phoneWidth, 0xFF888888);
        }

        if (photoFiles.size() > 1) {
            boolean hovL = mouseX >= phoneX + 6 && mouseX <= phoneX + 20
                        && mouseY >= arrowY - 2 && mouseY <= arrowY + 10;
            boolean hovR = mouseX >= phoneX + phoneWidth - 20 && mouseX <= phoneX + phoneWidth - 6
                        && mouseY >= arrowY - 2 && mouseY <= arrowY + 10;
            getFont().draw(stack, "<", phoneX + 8,               arrowY, hovL ? 0xFFFFFFFF : 0xFFAAAAAA);
            getFont().draw(stack, ">", phoneX + phoneWidth - 14, arrowY, hovR ? 0xFFFFFFFF : 0xFFAAAAAA);
        }

        String counter = (fullscreenIdx + 1) + " / " + photoFiles.size();
        PhoneRenderHelper.drawCenteredText(stack, getFont(), counter,
                phoneX, arrowY, phoneWidth, 0xFFCCCCCC);
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleBackButtonClick(mx, my)) return true;
        if (CameraModBridge.isLoaded() || button != 0) return false;
        if (photoFiles.isEmpty()) return false;

        if (view == View.GRID) {
            int idx = getThumbIndexAt(mx, my);
            if (idx >= 0) {
                fullscreenIdx = idx;
                view = View.FULLSCREEN;
                return true;
            }
        } else {
            int arrowY = phoneY + phoneHeight - 12;
            if (my >= arrowY - 2 && my <= arrowY + 10) {
                if (mx >= phoneX + 6 && mx <= phoneX + 20) {
                    fullscreenIdx = (fullscreenIdx - 1 + photoFiles.size()) % photoFiles.size();
                    return true;
                }
                if (mx >= phoneX + phoneWidth - 20 && mx <= phoneX + phoneWidth - 6) {
                    fullscreenIdx = (fullscreenIdx + 1) % photoFiles.size();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (CameraModBridge.isLoaded()) return false;
        if (view == View.GRID) {
            scrollOffset = Math.max(0, scrollOffset - (int) (delta * 12));
            return true;
        }
        if (view == View.FULLSCREEN && !photoFiles.isEmpty()) {
            int next = fullscreenIdx + (delta < 0 ? 1 : -1);
            fullscreenIdx = Math.max(0, Math.min(next, photoFiles.size() - 1));
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int getThumbIndexAt(double mx, double my) {
        int contentY = phoneY + BAR_H;
        int cols     = 2;
        int thumbW   = (phoneWidth - (cols + 1) * PADDING) / cols;
        int thumbH   = thumbW;
        for (int i = 0; i < photoFiles.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int tx  = phoneX + PADDING + col * (thumbW + PADDING);
            int ty  = contentY + PADDING + row * (thumbH + PADDING) - scrollOffset;
            if (mx >= tx && mx < tx + thumbW && my >= ty && my < ty + thumbH) return i;
        }
        return -1;
    }

    private ResourceLocation getOrLoadTexture(String filename) {
        if (textureCache.containsKey(filename)) return textureCache.get(filename);

        Minecraft mc = Minecraft.getInstance();
        File file = new File(mc.gameDirectory, CameraHelper.PHOTO_FOLDER + "/" + filename);
        if (!file.exists()) { textureCache.put(filename, null); return null; }

        try (FileInputStream fis = new FileInputStream(file)) {
            NativeImage img = NativeImage.read(fis);
            textureDims.put(filename, new int[]{ img.getWidth(), img.getHeight() });
            DynamicTexture dynTex = new DynamicTexture(img);
            String id = "districtlife_phone:gallery_" + (texCounter++);
            ResourceLocation rl = mc.getTextureManager().register(id, dynTex);
            textureCache.put(filename, rl);
            return rl;
        } catch (Exception e) {
            textureCache.put(filename, null);
            return null;
        }
    }

    private void drawEmpty(MatrixStack stack) {
        int contentY = phoneY + BAR_H;
        int contentH = phoneHeight - BAR_H;
        PhoneRenderHelper.fillRect(stack, phoneX, contentY, phoneWidth, contentH, 0xFF111111);
        PhoneRenderHelper.drawCenteredText(stack, getFont(), "Aucune photo",
                phoneX, phoneY + phoneHeight / 2 - 4, phoneWidth, 0xFF888888);
    }

    // -------------------------------------------------------------------------
    // Scissor
    // -------------------------------------------------------------------------

    private void enableScissor(int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int sx = (int)(x * scale);
        int sy = (int)((mc.getWindow().getGuiScaledHeight() - y - h) * scale);
        int sw = (int)(w * scale);
        int sh = (int)(h * scale);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    private void disableScissor() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }

    // -------------------------------------------------------------------------
    // Meta
    // -------------------------------------------------------------------------

    @Override
    public String getTitle() {
        return "Galerie";
    }
}
