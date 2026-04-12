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
 * Gere le rendu du cadre smartphone et la navigation entre les apps via un stack.
 *
 * Dimensions internes : 180 x 320 px (coordonnees GUI Minecraft).
 */
@OnlyIn(Dist.CLIENT)
public class PhoneScreen extends Screen {

    public static final int PHONE_WIDTH  = 180;
    public static final int PHONE_HEIGHT = 320;

    private static final ResourceLocation PHONE_FRAME =
            new ResourceLocation("districtlife_phone", "textures/gui/phone_frame.png");
    private static final ResourceLocation WALLPAPER =
            new ResourceLocation("districtlife_phone", "textures/gui/wallpaper_default.png");

    // Coordonnees de l'origine du cadre dans l'ecran GUI
    private int frameX;
    private int frameY;

    // Zone interne (contenu, sans le cadre)
    public int contentX;
    public int contentY;
    public static final int CONTENT_WIDTH  = 160;
    public static final int CONTENT_HEIGHT = 280;

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

        // Centrage du cadre
        frameX = (width  - PHONE_WIDTH)  / 2;
        frameY = (height - PHONE_HEIGHT) / 2;

        // Zone de contenu interne (cadre avec 10px de marge)
        contentX = frameX + 10;
        contentY = frameY + 20;

        // Demarre sur le lockscreen
        LockScreen lockScreen = new LockScreen();
        lockScreen.init(this, contentX, contentY, CONTENT_WIDTH, CONTENT_HEIGHT);
        currentApp = lockScreen;
        navStack.clear();
        navStack.push(lockScreen);
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTick) {
        // Fond obscurci
        PhoneRenderHelper.drawDimBackground(stack, width, height);

        // Wallpaper (fond de l'ecran interne)
        PhoneRenderHelper.drawTexture(stack, WALLPAPER,
                contentX, contentY, CONTENT_WIDTH, CONTENT_HEIGHT);

        // Cadre du smartphone par-dessus
        PhoneRenderHelper.drawTexture(stack, PHONE_FRAME,
                frameX, frameY, PHONE_WIDTH, PHONE_HEIGHT);

        // Rendu de l'app courante
        if (currentApp != null) {
            currentApp.render(stack, mouseX, mouseY, partialTick);
        }

        super.render(stack, mouseX, mouseY, partialTick);
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
        app.init(this, contentX, contentY, CONTENT_WIDTH, CONTENT_HEIGHT);
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
