package com.districtlife.phone.screen;

import com.districtlife.phone.call.CallSignal;
import com.districtlife.phone.call.PhoneCallState;
import com.districtlife.phone.call.PhoneCallState.CallState;
import com.districtlife.phone.network.PacketCallSignalFix;
import com.districtlife.phone.network.PacketHandler;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PhoneFixScreen extends Screen {

    private static final int W = 180;
    private static final int H = 200;

    private final String   fixPhone;
    private final BlockPos blockPos;

    // Etat initial transmis par le serveur
    private String pendingCaller;
    private String activeCall;

    // Saisie numpad (etat IDLE)
    private final StringBuilder dialInput = new StringBuilder();

    // Boutons d'action principaux
    private Button btnCall;
    private Button btnAccept;
    private Button btnDecline;
    private Button btnHangup;
    private Button btnBack;

    public PhoneFixScreen(String fixPhone, BlockPos blockPos,
                          String pendingCaller, String activeCall) {
        super(new StringTextComponent(""));
        this.fixPhone      = fixPhone;
        this.blockPos      = blockPos;
        this.pendingCaller = pendingCaller;
        this.activeCall    = activeCall;
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        int x = (width  - W) / 2;
        int y = (height - H) / 2;

        // Numpad (3x4 grid) - utilise pour l'etat IDLE
        String[] keys = {"1","2","3","4","5","6","7","8","9","*","0","#"};
        int kW = 38, kH = 18, kGapX = 6, kGapY = 4;
        int kStartX = x + (W - 3*kW - 2*kGapX) / 2;
        int kStartY = y + 72;
        for (int i = 0; i < 12; i++) {
            final String key = keys[i];
            int col = i % 3, row = i / 3;
            int bx = kStartX + col * (kW + kGapX);
            int by = kStartY + row * (kH + kGapY);
            addButton(new Button(bx, by, kW, kH, new StringTextComponent(key), btn -> {
                if (dialInput.length() < 15) dialInput.append(key);
            }));
        }

        int midX = x + W / 2;
        int bottomY = y + H - 30;

        // Bouton effacer (backspace)
        addButton(new Button(x + W - 44, y + 54, 40, 14,
                new StringTextComponent("<"), btn -> {
                    if (dialInput.length() > 0) dialInput.deleteCharAt(dialInput.length() - 1);
                }));

        // Appeler
        btnCall = addButton(new Button(midX - 55, bottomY, 110, 18,
                new StringTextComponent("Appeler"), btn -> startCall()));

        // Accepter / Refuser (etat RINGING)
        btnAccept = addButton(new Button(x + 14, bottomY, 66, 18,
                new StringTextComponent("Accepter"), btn -> acceptCall()));
        btnDecline = addButton(new Button(x + W - 80, bottomY, 66, 18,
                new StringTextComponent("Refuser"), btn -> declineCall()));

        // Raccrocher (etats CALLING / IN_CALL)
        btnHangup = addButton(new Button(midX - 45, bottomY, 90, 18,
                new StringTextComponent("Raccrocher"), btn -> hangup()));

        // Retour (ferme l'ecran)
        btnBack = addButton(new Button(x + 4, y + 4, 16, 12,
                new StringTextComponent("<"), btn -> onClose()));

        refreshButtons();
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    @Override
    public void tick() {
        // Synchronise l'etat de l'ecran avec PhoneCallState
        CallState state = PhoneCallState.getState();
        if (state == CallState.INCALL) {
            activeCall    = PhoneCallState.getOtherPhone();
            pendingCaller = "";
        } else if (state == CallState.CALLING) {
            pendingCaller = "";
            // activeCall conserve la cible composee
        } else if (state == CallState.RINGING) {
            pendingCaller = PhoneCallState.getOtherPhone();
            activeCall    = "";
        } else if (state == CallState.IDLE || state == CallState.IMPOSSIBLE) {
            if (!activeCall.isEmpty() || !pendingCaller.isEmpty()) {
                // L'appel vient de se terminer
                pendingCaller = "";
                activeCall    = "";
            }
        }
        refreshButtons();
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        renderBackground(ms);

        int x = (width  - W) / 2;
        int y = (height - H) / 2;

        // Fond
        fill(ms, x, y, x + W, y + H, 0xFF1A1A2E);
        fill(ms, x + 1, y + 1, x + W - 1, y + H - 1, 0xFF16213E);

        // Bordure haut
        fill(ms, x, y, x + W, y + 22, 0xFF0F3460);

        // Titre
        font.draw(ms, "Boitier Telephonique", x + 8, y + 7, 0xFF_A0C4FF);
        font.draw(ms, "N : " + fixPhone, x + 8, y + 26, 0xFF_AAAAAA);

        // Separateur
        fill(ms, x + 6, y + 38, x + W - 6, y + 39, 0xFF_334455);

        CallState state = PhoneCallState.getState();

        if (state == CallState.RINGING) {
            drawRingingState(ms, x, y);
        } else if (state == CallState.CALLING) {
            drawCallingState(ms, x, y);
        } else if (state == CallState.INCALL) {
            drawInCallState(ms, x, y);
        } else if (state == CallState.IMPOSSIBLE) {
            drawImpossibleState(ms, x, y);
        } else {
            drawIdleState(ms, x, y);
        }

        super.render(ms, mx, my, pt);
    }

    private void drawIdleState(MatrixStack ms, int x, int y) {
        // Affichage des chiffres composes
        String display = dialInput.toString();
        if (display.isEmpty()) display = "_";
        int tw = font.width(display);
        font.draw(ms, display, x + (W - tw) / 2f, y + 46, 0xFF_FFFFFF);
    }

    private void drawRingingState(MatrixStack ms, int x, int y) {
        String caller = pendingCaller.isEmpty() ? PhoneCallState.getOtherPhone() : pendingCaller;
        font.draw(ms, "Appel entrant :", x + 16, y + 46, 0xFF_AAAAAA);
        int tw = font.width(caller);
        font.draw(ms, caller, x + (W - tw) / 2f, y + 58, 0xFF_FFFFFF);
    }

    private void drawCallingState(MatrixStack ms, int x, int y) {
        String target = activeCall.isEmpty() ? dialInput.toString() : activeCall;
        font.draw(ms, "Appel en cours...", x + 16, y + 46, 0xFF_AAAAAA);
        int tw = font.width(target);
        font.draw(ms, target, x + (W - tw) / 2f, y + 58, 0xFF_FFFFFF);
    }

    private void drawInCallState(MatrixStack ms, int x, int y) {
        String partner = PhoneCallState.getOtherPhone();
        font.draw(ms, "En communication :", x + 16, y + 46, 0xFF_AAAAAA);
        int tw = font.width(partner);
        font.draw(ms, partner, x + (W - tw) / 2f, y + 58, 0xFF_00FF88);

        // Duree de l'appel
        if (minecraft != null && minecraft.player != null) {
            long elapsed = (minecraft.player.level.getGameTime() - PhoneCallState.getCallStartTick()) / 20;
            String dur = String.format("%02d:%02d", elapsed / 60, elapsed % 60);
            int dw = font.width(dur);
            font.draw(ms, dur, x + (W - dw) / 2f, y + 70, 0xFF_888888);
        }
    }

    private void drawImpossibleState(MatrixStack ms, int x, int y) {
        String msg = "Numero indisponible";
        int tw = font.width(msg);
        font.draw(ms, msg, x + (W - tw) / 2f, y + 52, 0xFF_FF4444);
    }

    // -------------------------------------------------------------------------
    // Visibilite des boutons
    // -------------------------------------------------------------------------

    private void refreshButtons() {
        CallState state = PhoneCallState.getState();
        boolean idle      = (state == CallState.IDLE || state == CallState.IMPOSSIBLE);
        boolean ringing   = (state == CallState.RINGING);
        boolean active    = (state == CallState.CALLING || state == CallState.INCALL);

        btnCall.visible    = idle;
        btnCall.active     = idle && dialInput.length() >= 2;

        btnAccept.visible  = ringing;
        btnDecline.visible = ringing;

        btnHangup.visible  = active;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void startCall() {
        String target = dialInput.toString().replaceAll("[^0-9]", "");
        if (target.length() < 2) return;
        PhoneCallState.setCalling(target, target);
        activeCall = target;
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignalFix(CallSignal.CALL, fixPhone, target, blockPos));
    }

    private void acceptCall() {
        String caller = pendingCaller.isEmpty() ? PhoneCallState.getOtherPhone() : pendingCaller;
        PhoneCallState.setInCall(0);
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignalFix(CallSignal.ACCEPT, fixPhone, caller, blockPos));
    }

    private void declineCall() {
        String caller = pendingCaller.isEmpty() ? PhoneCallState.getOtherPhone() : pendingCaller;
        PhoneCallState.reset();
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignalFix(CallSignal.DECLINE, fixPhone, caller, blockPos));
    }

    private void hangup() {
        PhoneCallState.reset();
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignalFix(CallSignal.HANGUP, fixPhone, "", blockPos));
    }

    // -------------------------------------------------------------------------
    // Fermeture
    // -------------------------------------------------------------------------

    @Override
    public void onClose() {
        // Raccrocher automatiquement si un appel est en cours
        CallState state = PhoneCallState.getState();
        if (state == CallState.CALLING || state == CallState.INCALL) {
            hangup();
        } else if (state == CallState.RINGING) {
            declineCall();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
