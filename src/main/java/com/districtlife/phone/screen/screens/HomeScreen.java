package com.districtlife.phone.screen.screens;

import com.districtlife.phone.news.NewsClientCache;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.screen.widgets.StatusBar;
import com.districtlife.phone.util.PhoneFont;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@OnlyIn(Dist.CLIENT)
public class HomeScreen extends AbstractPhoneApp {

    // Garde pour AppSettings (toggles d'apps)
    public static final Object[][] ALL_APPS = {
            { "Telephone",  "app_phone",    AppPhone.class,    null },
            { "SMS",        "app_sms",      AppSMS.class,      null },
            { "Contacts",   "app_contacts", AppContacts.class, null },
            { "Map",        "app_map",      AppMap.class,      null },
            { "News",       "app_news",     AppNews.class,     (BooleanSupplier) NewsClientCache::hasUnread },
            { "Appareil",   "app_camera",   AppCamera.class,   null },
            { "Galerie",    "app_gallery",  AppGallery.class,  null },
            { "Parametres", "app_settings", AppSettings.class, null },
    };

    // Grille centrale 1x4 : { label, chemin texture (sans prefix/extension), classe app }
    private static final Object[][] GRID_DEFS = {
            { "Maps",     "accueil/screenmiddle/app_maps/maps",         AppMap.class      },
            { "News",     "accueil/screenmiddle/app_news/news",         AppNews.class     },
            { "Appareil", "accueil/screenmiddle/app_appareil/appareil", AppCamera.class   },
            { "Photo",    "accueil/screenmiddle/app_galerie/galerie",   AppGallery.class  },
    };

    private static final int GRID_COLS = 4;

    // Texture de fond du dock
    private static final ResourceLocation TEX_DOCK_BG = new ResourceLocation(
            "districtlife_phone", "textures/gui/phone/accueil/screendown/dock/doc.png");

    // Dock bas : { chemin texture, classe app }
    private static final Object[][] DOCK_DEFS = {
            { "accueil/screendown/dock/appel",     AppPhone.class    },
            { "accueil/screendown/dock/sms",       AppSMS.class      },
            { "accueil/screendown/dock/contacte",  AppContacts.class },
            { "accueil/screendown/dock/parametre", AppSettings.class },
    };

    // Dimensions de reference (content 424x900)
    private static final int GRID_START_Y_REF = 130;
    private static final int DOCK_ICON_REF      = 66;  // -8% vs 72
    private static final int DOCK_BG_Y_REF      = 770;
    private static final int DOCK_BG_W_REF      = 374;
    private static final int DOCK_BG_H_REF      = 105;
    private static final int DOCK_ICON_TOP_REF  = 21;  // px depuis le top du doc
    private static final int DOCK_ICON_LEFT_REF = 20;  // px depuis le left du doc (1ere app)
    private static final int DOCK_GAP_REF       = 26;  // interval entre apps

    // Donnees d'instances (remplies dans onInit)
    private final List<int[]>              gridBounds   = new ArrayList<>();
    private final List<ResourceLocation>   gridTex      = new ArrayList<>();
    private final List<String>             gridLabels   = new ArrayList<>();
    private final List<AbstractPhoneApp>   gridApps     = new ArrayList<>();

    private final List<int[]>              dockBounds   = new ArrayList<>();
    private final List<ResourceLocation>   dockTex      = new ArrayList<>();
    private final List<AbstractPhoneApp>   dockApps     = new ArrayList<>();

    private int dockBgX, dockBgY, dockBgX2, dockBgY2;
    private StatusBar statusBar;

    @Override
    @SuppressWarnings("unchecked")
    protected void onInit() {
        float s = phoneWidth / 424.0f;

        statusBar = new StatusBar(phoneX, phoneY, phoneWidth, phoneHeight, true);

        // Taille et espacement communs (grille + dock sur memes colonnes)
        int iconSize = Math.max(1, (int)(DOCK_ICON_REF * s));
        int dGap     = Math.max(0, (int)(DOCK_GAP_REF * s));

        // --- Dock background ---
        int dBgW    = Math.max(1, (int)(DOCK_BG_W_REF * s));
        int dBgH    = Math.max(1, (int)(DOCK_BG_H_REF * s));
        int dBgX    = phoneX + (phoneWidth - dBgW) / 2;
        int dBgY    = phoneY + (int)(DOCK_BG_Y_REF * s);
        int dIconY  = dBgY + (int)(DOCK_ICON_TOP_REF * s);
        int dFirstX = dBgX + (int)(DOCK_ICON_LEFT_REF * s);

        dockBgX  = dBgX;
        dockBgY  = dBgY;
        dockBgX2 = dBgX + dBgW;
        dockBgY2 = dBgY + dBgH;

        // Positions X partagees (4 colonnes identiques pour grille et dock)
        int[] colX = new int[GRID_COLS];
        for (int i = 0; i < GRID_COLS; i++) {
            colX[i] = dFirstX + i * (iconSize + dGap);
        }

        // --- Grille centrale ---
        gridBounds.clear(); gridTex.clear(); gridLabels.clear(); gridApps.clear();
        int gridSY = phoneY + (int)(GRID_START_Y_REF * s);

        for (int i = 0; i < GRID_DEFS.length; i++) {
            gridBounds.add(new int[]{ colX[i % GRID_COLS], gridSY, iconSize, iconSize });
            gridTex.add(new ResourceLocation("districtlife_phone",
                    "textures/gui/phone/" + GRID_DEFS[i][1] + ".png"));
            gridLabels.add((String) GRID_DEFS[i][0]);
            try { gridApps.add(((Class<? extends AbstractPhoneApp>) GRID_DEFS[i][2]).newInstance()); }
            catch (Exception e) { gridApps.add(null); }
        }

        // --- Dock ---
        dockBounds.clear(); dockTex.clear(); dockApps.clear();

        for (int i = 0; i < DOCK_DEFS.length; i++) {
            dockBounds.add(new int[]{ colX[i], dIconY, iconSize, iconSize });
            dockTex.add(new ResourceLocation("districtlife_phone",
                    "textures/gui/phone/" + DOCK_DEFS[i][0] + ".png"));
            try { dockApps.add(((Class<? extends AbstractPhoneApp>) DOCK_DEFS[i][1]).newInstance()); }
            catch (Exception e) { dockApps.add(null); }
        }
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (statusBar != null) statusBar.render(stack, mouseX, mouseY, partialTicks);

        // Grille centrale
        float s = phoneWidth / 424.0f;
        float labelMaxH = phoneHeight * 0.016f / 8.0f;
        float labelMaxW = phoneWidth  * 0.08f  / Math.max(1, PhoneFont.widthNoGap("Appareil"));
        float labelScale = Math.min(labelMaxH, labelMaxW);
        for (int i = 0; i < gridBounds.size(); i++) {
            int[] b = gridBounds.get(i);
            PhoneRenderHelper.drawTexture(stack, gridTex.get(i), b[0], b[1], b[2], b[3]);
            String label = gridLabels.get(i);
            int labelW = PhoneFont.widthNoGap(label);
            float labelX = b[0] + (b[2] - labelW * labelScale) / 2.0f;
            float labelY = b[1] + b[3] + 3 * s;
            stack.pushPose();
            stack.translate(labelX, labelY, 0);
            stack.scale(labelScale, labelScale, 1.0f);
            PhoneFont.drawNoGap(stack, label, 0, 0, 0xFFFFFFFF);
            stack.popPose();
        }

        // Fond du dock (texture doc.png)
        PhoneRenderHelper.drawTexture(stack, TEX_DOCK_BG, dockBgX, dockBgY, dockBgX2 - dockBgX, dockBgY2 - dockBgY);

        // Icones du dock
        for (int i = 0; i < dockBounds.size(); i++) {
            int[] b = dockBounds.get(i);
            PhoneRenderHelper.drawTexture(stack, dockTex.get(i), b[0], b[1], b[2], b[3]);
        }
    }

    @Override
    public String getTitle() { return "Accueil"; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < gridBounds.size(); i++) {
            int[] b = gridBounds.get(i);
            if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                AbstractPhoneApp app = gridApps.get(i);
                if (app != null) phoneScreen.navigateTo(app);
                return true;
            }
        }
        for (int i = 0; i < dockBounds.size(); i++) {
            int[] b = dockBounds.get(i);
            if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                AbstractPhoneApp app = dockApps.get(i);
                if (app != null) phoneScreen.navigateTo(app);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBack() {}
}
