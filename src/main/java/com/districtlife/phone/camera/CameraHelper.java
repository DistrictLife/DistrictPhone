package com.districtlife.phone.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Gere la capture d'ecran differee pour l'app camera du telephone.
 *
 * Flux :
 *   1. AppCamera appelle requestCapture()
 *   2. Le GUI se ferme immediatement pour laisser la scene visible
 *   3. ClientTickEvent decremente le compteur (3 ticks)
 *   4. RenderTickEvent.END capture le frame via ScreenShotHelper
 *   5. La photo est sauvegardee dans screenshots/phone/photo_<timestamp>.png
 *      → accessible depuis AppGallery qui scanne ce dossier
 */
@OnlyIn(Dist.CLIENT)
public final class CameraHelper {

    /** Dossier de destination des photos dans le repertoire du jeu. */
    public static final String PHOTO_FOLDER = "screenshots/phone";

    private static boolean pending        = false;
    private static int     countdown      = 0;
    private static boolean readyToCapture = false;

    private CameraHelper() {}

    // -------------------------------------------------------------------------
    // API publique
    // -------------------------------------------------------------------------

    public static void requestCapture() {
        pending        = true;
        countdown      = 3;
        readyToCapture = false;
        Minecraft.getInstance().setScreen(null);
    }

    public static boolean isPending() {
        return pending || readyToCapture;
    }

    // -------------------------------------------------------------------------
    // TickEvent.ClientTickEvent END — logique jeu uniquement
    // -------------------------------------------------------------------------

    public static void onClientTick() {
        if (!pending || countdown <= 0) return;
        countdown--;
        if (countdown <= 0) {
            readyToCapture = true;
            pending = false;
        }
    }

    // -------------------------------------------------------------------------
    // TickEvent.RenderTickEvent END — capture quand le frame est complet
    // -------------------------------------------------------------------------

    public static void onRenderTick() {
        if (!readyToCapture) return;
        readyToCapture = false;

        Minecraft mc = Minecraft.getInstance();
        Framebuffer fb = mc.getMainRenderTarget();

        NativeImage image = ScreenShotHelper.takeScreenshot(
                mc.getWindow().getWidth(),
                mc.getWindow().getHeight(),
                fb);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        String filename  = "photo_" + timestamp + ".png";
        File   target    = new File(mc.gameDirectory, PHOTO_FOLDER + "/" + filename);
        target.getParentFile().mkdirs();

        try {
            image.writeToFile(target);
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new StringTextComponent("\u00A7aPhoto enregistree !"), true);
            }
        } catch (IOException e) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new StringTextComponent("\u00A7cEchec de la sauvegarde photo."), true);
            }
        } finally {
            image.close();
        }
    }
}
