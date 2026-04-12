package com.districtlife.phone.camera;

import net.minecraftforge.fml.ModList;

/**
 * Verifie la presence du Camera Mod (henkelmax) sans charger ses classes.
 * Safe a charger sur serveur dedie et quand le mod est absent.
 */
public final class CameraModBridge {

    public static final String CAMERA_MODID = "camera";

    private CameraModBridge() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(CAMERA_MODID);
    }
}
