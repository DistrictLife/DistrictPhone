package com.districtlife.phone.screen.screens;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.news.NewsClientCache;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.screen.PhoneScreen;
import com.districtlife.phone.screen.widgets.AppIcon;
import com.districtlife.phone.screen.widgets.StatusBar;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@OnlyIn(Dist.CLIENT)
public class HomeScreen extends AbstractPhoneApp {

    private static final int COLUMNS      = 3;
    private static final int ICON_SIZE    = 40;
    private static final int ICON_PADDING = 10;

    /**
     * Definition statique de toutes les apps disponibles sur l'ecran d'accueil.
     * Format : { label, iconId, app, notifSupplier (optionnel) }
     * Cette liste est utilisee par HomeScreen (rendu) et AppSettings (toggles).
     * "app_settings" n'est jamais masquee.
     */
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

    private final List<AppIcon> appIcons = new ArrayList<>();
    private StatusBar statusBar;

    @Override
    protected void onInit() {
        appIcons.clear();

        ItemStack phoneStack = getPhoneStack();

        int statusBarHeight = 14;
        int gridStartY = phoneY + statusBarHeight + 8;
        int gridStartX = phoneX + (phoneWidth - COLUMNS * (ICON_SIZE + ICON_PADDING) + ICON_PADDING) / 2;

        int visibleIdx = 0;
        for (Object[] def : ALL_APPS) {
            String appId = (String) def[1];

            // Parametres ne peut jamais etre masque
            boolean hidden = !"app_settings".equals(appId)
                    && PhoneData.isAppHidden(phoneStack, appId);
            if (hidden) continue;

            String label = (String) def[0];
            Class<? extends AbstractPhoneApp> appClass =
                    (Class<? extends AbstractPhoneApp>) def[2];
            BooleanSupplier notif = (BooleanSupplier) def[3];

            AbstractPhoneApp app;
            try { app = appClass.newInstance(); }
            catch (Exception e) { continue; }

            int col  = visibleIdx % COLUMNS;
            int row  = visibleIdx / COLUMNS;
            int iconX = gridStartX + col * (ICON_SIZE + ICON_PADDING);
            int iconY = gridStartY + row * (ICON_SIZE + ICON_PADDING + 10);

            appIcons.add(new AppIcon(iconX, iconY, ICON_SIZE, label, appId, app, notif));
            visibleIdx++;
        }

        statusBar = new StatusBar(phoneX, phoneY, phoneWidth, statusBarHeight);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (statusBar != null) statusBar.render(stack, mouseX, mouseY, partialTicks);
        for (AppIcon icon : appIcons) icon.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public String getTitle() { return "Accueil"; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AppIcon icon : appIcons) {
            if (icon.isHovered(mouseX, mouseY)) {
                phoneScreen.navigateTo(icon.getApp());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBack() {
        // L'accueil ne revient nulle part - Echap ferme le GUI (gere par PhoneScreen)
    }
}
