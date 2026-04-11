package com.districtlife.phone.screen.screens;

import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.screen.PhoneScreen;
import com.districtlife.phone.screen.widgets.AppIcon;
import com.districtlife.phone.screen.widgets.StatusBar;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HomeScreen extends AbstractPhoneApp {

    private static final int COLUMNS = 3;
    private static final int ICON_SIZE = 40;
    private static final int ICON_PADDING = 10;

    private final List<AppIcon> appIcons = new ArrayList<>();
    private StatusBar statusBar;

    @Override
    protected void onInit() {
        appIcons.clear();

        // Definition des apps dans l'ordre de la grille
        Object[][] apps = {
                { "Telephone",  "app_phone",    new AppPhone()    },
                { "SMS",        "app_sms",      new AppSMS()      },
                { "Contacts",   "app_contacts", new AppContacts() },
                { "Map",        "app_map",      new AppMap()      },
                { "News",       "app_news",     new AppNews()     },
                { "Appareil",   "app_camera",   new AppCamera()   },
                { "Galerie",    "app_gallery",  new AppGallery()  },
                { "Parametres", "app_settings", new AppSettings() },
        };

        int statusBarHeight = 14;
        int gridStartY = phoneY + statusBarHeight + 8;
        int gridStartX = phoneX + (phoneWidth - COLUMNS * (ICON_SIZE + ICON_PADDING) + ICON_PADDING) / 2;

        for (int i = 0; i < apps.length; i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int iconX = gridStartX + col * (ICON_SIZE + ICON_PADDING);
            int iconY = gridStartY + row * (ICON_SIZE + ICON_PADDING + 10);

            String label    = (String) apps[i][0];
            String iconName = (String) apps[i][1];
            AbstractPhoneApp app = (AbstractPhoneApp) apps[i][2];

            appIcons.add(new AppIcon(iconX, iconY, ICON_SIZE, label, iconName, app));
        }

        statusBar = new StatusBar(phoneX, phoneY, phoneWidth, statusBarHeight);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        // Barre de statut
        if (statusBar != null) {
            statusBar.render(stack, mouseX, mouseY, partialTicks);
        }

        // Icones des apps
        for (AppIcon icon : appIcons) {
            icon.render(stack, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public String getTitle() {
        return "Accueil";
    }

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
