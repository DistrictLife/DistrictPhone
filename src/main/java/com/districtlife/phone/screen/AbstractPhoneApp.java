package com.districtlife.phone.screen;

import com.districtlife.phone.item.PhoneItem;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.districtlife.phone.util.PhoneFont;

/**
 * Classe de base pour toutes les applications du telephone.
 * Chaque app est rendue a l'interieur du {@link PhoneScreen}.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractPhoneApp {

    private static final ResourceLocation TEX_TITLE_BAR =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/title_bar.png");

    protected PhoneScreen phoneScreen;

    /** Zone interne du telephone (coordonnees ecran) */
    protected int phoneX;
    protected int phoneY;
    protected int phoneWidth;
    protected int phoneHeight;

    public void init(PhoneScreen phoneScreen, int phoneX, int phoneY,
                     int phoneWidth, int phoneHeight) {
        this.phoneScreen = phoneScreen;
        this.phoneX = phoneX;
        this.phoneY = phoneY;
        this.phoneWidth = phoneWidth;
        this.phoneHeight = phoneHeight;
        onInit();
    }

    /** Appele apres l'initialisation - override pour setup des widgets. */
    protected void onInit() {}

    /** Rendu principal de l'app. */
    public abstract void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks);

    /** Titre affiche dans la barre de titre de l'app. */
    public abstract String getTitle();

    /** Retourne a l'ecran d'accueil. */
    public void onBack() {
        phoneScreen.navigateTo(phoneScreen.getHomeScreen());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) { return false; }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    public boolean charTyped(char codePoint, int modifiers) { return false; }
    public void tick() {}

    protected FontRenderer getFont() {
        return com.districtlife.phone.util.PhoneFont.fr();
    }

    /**
     * Retourne l'ItemStack du telephone actuellement ouvert depuis l'inventaire local.
     * Utilise le numero de telephone connu du PhoneScreen comme cle de recherche.
     */
    protected ItemStack getPhoneStack() {
        return PhoneItem.findPhoneStack(
                net.minecraft.client.Minecraft.getInstance().player,
                phoneScreen.getPhoneNumber());
    }

    /**
     * Dessine la barre de titre standard avec bouton retour " <- ".
     * Retourne la hauteur occupee par la barre (a utiliser pour decaler le contenu).
     */
    protected int drawTitleBar(MatrixStack stack, int mouseX, int mouseY) {
        int barHeight = 16;
        int textColor = 0xFFFFFFFF;

        // fond de la barre
        PhoneRenderHelper.drawTexture(stack, TEX_TITLE_BAR, phoneX, phoneY, phoneWidth, barHeight);

        // bouton retour
        PhoneFont.draw(stack, "\u2190", phoneX + 4, phoneY + 4, textColor);

        // titre centre
        PhoneFont.drawCentered(stack, getTitle(), phoneX, phoneY + 4, phoneWidth, textColor);

        return barHeight;
    }

    /**
     * Gere le clic sur le bouton retour de la barre de titre.
     * Appeler dans mouseClicked() des sous-classes.
     */
    protected boolean handleBackButtonClick(double mouseX, double mouseY) {
        if (mouseX >= phoneX && mouseX <= phoneX + 14
                && mouseY >= phoneY && mouseY <= phoneY + 16) {
            onBack();
            return true;
        }
        return false;
    }

    /**
     * Dessine un placeholder centre " Bientot disponible ".
     */
    protected void drawPlaceholder(MatrixStack stack) {
        String text = "Bientot disponible";
        int textWidth = PhoneFont.width(text);
        int x = phoneX + (phoneWidth - textWidth) / 2;
        int y = phoneY + phoneHeight / 2 - 4;
        PhoneFont.draw(stack, text, x, y, 0xFF888888);
    }
}
