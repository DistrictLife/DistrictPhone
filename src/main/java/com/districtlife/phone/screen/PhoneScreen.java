package com.districtlife.phone.screen;

import com.districtlife.phone.screen.screens.HomeScreen;
import com.districtlife.phone.screen.screens.LockScreen;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Ecran principal du telephone.
 * Toutes les textures sont rendues a leurs dimensions exactes (pixels 1:1).
 *
 * cadre_phone.png : 455x926
 * background.png  : 424x900  (centree dans le cadre : marge 15px H, 13px V)
 */
@OnlyIn(Dist.CLIENT)
public class PhoneScreen extends Screen {

    // Dimensions exactes du cadre PNG
    public static final int PHONE_WIDTH  = 455;
    public static final int PHONE_HEIGHT = 926;

    // Dimensions exactes du background PNG
    public static final int CONTENT_WIDTH  = 424;
    public static final int CONTENT_HEIGHT = 900;

    // Decalage du background dans le cadre
    private static final int BG_OFFSET_X = (PHONE_WIDTH  - CONTENT_WIDTH)  / 2; // 15
    private static final int BG_OFFSET_Y = (PHONE_HEIGHT - CONTENT_HEIGHT) / 2; // 13

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/background.png");
    private static final ResourceLocation CADRE =
            new ResourceLocation("districtlife_phone", "textures/gui/phone/cadre_phone.png");

    // Coordonnees de l'origine du cadre dans l'ecran GUI
    private int frameX;
    private int frameY;

    // Origine de la zone de contenu (= background)
    public int contentX;
    public int contentY;

    // Dimensions rendues apres scaling dynamique
    private int renderPhoneW;
    private int renderPhoneH;
    private int renderContentW;
    private int renderContentH;

    /** Numero de telephone de cet item (lu depuis le NBT a l'ouverture). */
    private final String phoneNumber;

    private final Deque<AbstractPhoneApp> navStack = new ArrayDeque<>();
    private AbstractPhoneApp currentApp;
    private final HomeScreen homeScreen;

    public PhoneScreen(String phoneNumber) {
        super(new StringTextComponent("Phone"));
        this.phoneNumber = phoneNumber;
        this.homeScreen  = new HomeScreen();
    }

    /** Ouvre le PhoneScreen depuis le client (appele via DistExecutor). */
    public static void open(String phoneNumber) {
        Minecraft.getInstance().setScreen(new PhoneScreen(phoneNumber));
    }

    @Override
    protected void init() {
        super.init();

        // Scale dynamique : telephone remplit ~90% de la hauteur ecran
        float scaleH = (height - 20f) / PHONE_HEIGHT;
        float scaleW = (width  - 20f) / PHONE_WIDTH;
        float scale  = Math.min(scaleH, scaleW);

        renderPhoneW   = (int)(PHONE_WIDTH   * scale);
        renderPhoneH   = (int)(PHONE_HEIGHT  * scale);
        renderContentW = (int)(CONTENT_WIDTH * scale);
        renderContentH = (int)(CONTENT_HEIGHT * scale);

        int offsetX = (renderPhoneW - renderContentW) / 2;
        int offsetY = (renderPhoneH - renderContentH) / 2;

        frameX   = (width  - renderPhoneW) / 2;
        frameY   = (height - renderPhoneH) / 2;
        contentX = frameX + offsetX;
        contentY = frameY + offsetY;

        // Demarre sur le lockscreen
        LockScreen lockScreen = new LockScreen();
        lockScreen.init(this, contentX, contentY, renderContentW, renderContentH);
        currentApp = lockScreen;
        navStack.clear();
        navStack.push(lockScreen);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTick) {
        // Fond obscurci
        PhoneRenderHelper.drawDimBackground(stack, width, height);

        // 1 - Background scale dynamiquement
        PhoneRenderHelper.drawTexture(stack, BACKGROUND,
                contentX, contentY, renderContentW, renderContentH);

        // 2 - Rendu de l'app courante (par-dessus le background)
        if (currentApp != null) {
            currentApp.render(stack, mouseX, mouseY, partialTick);
        }

        super.render(stack, mouseX, mouseY, partialTick);

        // 3 - Cadre scale dynamiquement, par-dessus tout
        PhoneRenderHelper.drawTexture(stack, CADRE,
                frameX, frameY, renderPhoneW, renderPhoneH);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentApp != null && currentApp.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (currentApp != null && currentApp.mouseDragged(mouseX, mouseY, button, dx, dy)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (currentApp != null && currentApp.mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentApp != null && currentApp.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (currentApp != null && currentApp.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (currentApp != null && currentApp.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        if (currentApp != null) currentApp.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- Navigation ---

    public void navigateTo(AbstractPhoneApp app) {
        app.init(this, contentX, contentY, renderContentW, renderContentH);
        navStack.push(app);
        currentApp = app;
    }

    public void navigateBack() {
        if (navStack.size() > 1) {
            navStack.pop();
            currentApp = navStack.peek();
        } else {
            onClose();
        }
    }

    public HomeScreen getHomeScreen() { return homeScreen; }
    public String getPhoneNumber()    { return phoneNumber; }
    public int getFrameX()            { return frameX; }
    public int getFrameY()            { return frameY; }
}
