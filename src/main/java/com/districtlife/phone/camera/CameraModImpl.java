package com.districtlife.phone.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Passerelle CLIENT-ONLY vers le Camera Mod de henkelmax via reflexion.
 *
 * triggerCapture()  → ImageTaker.takeScreenshot(randomUUID)
 *                     Ferme l'ecran, cache le HUD, capture au prochain render tick,
 *                     envoie au serveur Camera Mod → item Image dans l'inventaire.
 *
 * openAlbumScreen() → AlbumScreen(List<UUID>) avec les images de l'inventaire joueur.
 *
 * sendToMod()       → ImageProcessor.sendScreenshotThreaded (usage direct si besoin).
 */
@OnlyIn(Dist.CLIENT)
public final class CameraModImpl {

    private static volatile Method cachedTakeScreenshot = null;
    private static volatile Method cachedSendMethod     = null;
    private static volatile Method cachedGetImageID     = null;
    private static volatile boolean takeLookupDone      = false;
    private static volatile boolean sendLookupDone      = false;
    private static volatile boolean albumLookupDone     = false;

    private CameraModImpl() {}

    // -------------------------------------------------------------------------
    // Capture via le pipeline natif Camera Mod
    // -------------------------------------------------------------------------

    /**
     * Declenche une capture via ImageTaker.takeScreenshot(uuid) :
     *   1. hideGui = true  (pas de HUD sur la photo)
     *   2. Ferme l'ecran courant (mc.setScreen(null))
     *   3. Au prochain RenderTickEvent.END : capture + envoi serveur Camera Mod
     *      → un item Image (camera:image) apparait dans l'inventaire du joueur.
     *
     * L'UUID est aleatoire (identifiant de l'image sur le serveur Camera Mod).
     */
    public static void triggerCapture() {
        Method method = resolveTakeScreenshot();
        if (method == null) return;
        try {
            method.invoke(null, UUID.randomUUID());
        } catch (Exception e) {
            // Camera Mod absent ou API modifiee — fallback gere par l'appelant
        }
    }

    /**
     * Retourne true si la methode ImageTaker.takeScreenshot est disponible.
     * Utilise pour savoir si Camera Mod peut gerer la capture.
     */
    public static boolean isTriggerAvailable() {
        return resolveTakeScreenshot() != null;
    }

    // -------------------------------------------------------------------------
    // Ouverture de l'album (AlbumScreen)
    // -------------------------------------------------------------------------

    /**
     * Collecte tous les items "camera:image" de l'inventaire du joueur,
     * recupere leurs UUID via ImageData.getImageID(), puis ouvre
     * AlbumScreen(List<UUID>) — l'ecran album natif de Camera Mod.
     */
    public static void openAlbumScreen() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            List<UUID> uuids = collectImageUUIDs(mc);

            Class<?> albumCls = Class.forName("de.maxhenkel.camera.gui.AlbumScreen");
            Constructor<?> ctor = albumCls.getConstructor(List.class);
            Screen screen = (Screen) ctor.newInstance(uuids);
            mc.setScreen(screen);
        } catch (Exception e) {
            // Camera Mod absent ou API modifiee
        }
    }

    private static List<UUID> collectImageUUIDs(Minecraft mc) {
        List<UUID> uuids = new ArrayList<>();
        Method getImageID = resolveGetImageID();
        if (getImageID == null) return uuids;

        for (int i = 0; i < mc.player.inventory.getContainerSize(); i++) {
            ItemStack stack = mc.player.inventory.getItem(i);
            if (stack.isEmpty()) continue;
            ResourceLocation regName = stack.getItem().getRegistryName();
            if (regName == null) continue;
            if (!"camera".equals(regName.getNamespace()) || !"image".equals(regName.getPath())) continue;
            try {
                UUID id = (UUID) getImageID.invoke(null, stack);
                if (id != null) uuids.add(id);
            } catch (Exception ignored) {}
        }
        return uuids;
    }

    // -------------------------------------------------------------------------
    // Envoi direct d'une NativeImage (usage optionnel)
    // -------------------------------------------------------------------------

    public static void sendToMod(UUID imageUuid, NativeImage image) {
        Method method = resolveSendMethod();
        if (method == null) { image.close(); return; }
        try {
            method.invoke(null, imageUuid, image);
        } catch (Exception e) {
            image.close();
        }
    }

    // -------------------------------------------------------------------------
    // Resolution des methodes (double-checked locking)
    // -------------------------------------------------------------------------

    private static Method resolveTakeScreenshot() {
        if (!takeLookupDone) {
            synchronized (CameraModImpl.class) {
                if (!takeLookupDone) {
                    try {
                        Class<?> cls = Class.forName("de.maxhenkel.camera.ImageTaker");
                        cachedTakeScreenshot = cls.getMethod("takeScreenshot", UUID.class);
                    } catch (Exception e) {
                        cachedTakeScreenshot = null;
                    }
                    takeLookupDone = true;
                }
            }
        }
        return cachedTakeScreenshot;
    }

    private static Method resolveSendMethod() {
        if (!sendLookupDone) {
            synchronized (CameraModImpl.class) {
                if (!sendLookupDone) {
                    try {
                        Class<?> cls = Class.forName("de.maxhenkel.camera.ImageProcessor");
                        cachedSendMethod = cls.getMethod(
                                "sendScreenshotThreaded", UUID.class, NativeImage.class);
                    } catch (Exception e) {
                        cachedSendMethod = null;
                    }
                    sendLookupDone = true;
                }
            }
        }
        return cachedSendMethod;
    }

    private static Method resolveGetImageID() {
        if (!albumLookupDone) {
            synchronized (CameraModImpl.class) {
                if (!albumLookupDone) {
                    try {
                        Class<?> cls = Class.forName("de.maxhenkel.camera.ImageData");
                        cachedGetImageID = cls.getMethod("getImageID", ItemStack.class);
                    } catch (Exception e) {
                        cachedGetImageID = null;
                    }
                    albumLookupDone = true;
                }
            }
        }
        return cachedGetImageID;
    }
}
