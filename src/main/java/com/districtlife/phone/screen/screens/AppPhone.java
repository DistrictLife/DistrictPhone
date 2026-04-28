package com.districtlife.phone.screen.screens;

import com.districtlife.phone.call.CallLogClient;
import com.districtlife.phone.call.CallLogEntry;
import com.districtlife.phone.call.CallSignal;
import com.districtlife.phone.call.PhoneCallState;
import com.districtlife.phone.call.PhoneCallState.CallState;
import com.districtlife.phone.capability.Contact;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.network.PacketCallSignal;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.registry.ModSounds;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import com.districtlife.phone.util.PhoneFont;

@OnlyIn(Dist.CLIENT)
public class AppPhone extends AbstractPhoneApp {

    // -------------------------------------------------------------------------
    // Textures GUI
    // -------------------------------------------------------------------------

    private static final ResourceLocation TEX_TAB_BG        = new ResourceLocation("districtlife_phone", "textures/gui/phone/tab_bg.png");
    private static final ResourceLocation TEX_TAB_NORMAL    = new ResourceLocation("districtlife_phone", "textures/gui/phone/tab_normal.png");
    private static final ResourceLocation TEX_TAB_SELECTED  = new ResourceLocation("districtlife_phone", "textures/gui/phone/tab_selected.png");
    private static final ResourceLocation TEX_TAB_HOVER     = new ResourceLocation("districtlife_phone", "textures/gui/phone/tab_hover.png");
    private static final ResourceLocation TEX_TAB_UNDERLINE = new ResourceLocation("districtlife_phone", "textures/gui/phone/tab_underline.png");
    private static final ResourceLocation TEX_TAB_SEPARATOR = new ResourceLocation("districtlife_phone", "textures/gui/phone/tab_separator.png");
    private static final ResourceLocation TEX_NUMBER_DISPLAY    = new ResourceLocation("districtlife_phone", "textures/gui/phone/number_display.png");
    private static final ResourceLocation TEX_BTN_KEY           = new ResourceLocation("districtlife_phone", "textures/gui/phone/btn_key.png");
    private static final ResourceLocation TEX_BTN_KEY_DEL       = new ResourceLocation("districtlife_phone", "textures/gui/phone/btn_key_del.png");
    private static final ResourceLocation TEX_BTN_CALL          = new ResourceLocation("districtlife_phone", "textures/gui/phone/btn_call.png");
    private static final ResourceLocation TEX_BTN_CALL_DISABLED = new ResourceLocation("districtlife_phone", "textures/gui/phone/btn_call_disabled.png");
    private static final ResourceLocation TEX_JOURNAL_ROW_EVEN  = new ResourceLocation("districtlife_phone", "textures/gui/phone/journal_row_even.png");
    private static final ResourceLocation TEX_JOURNAL_ROW_ODD   = new ResourceLocation("districtlife_phone", "textures/gui/phone/journal_row_odd.png");
    private static final ResourceLocation TEX_BTN_RECALL  = new ResourceLocation("districtlife_phone", "textures/gui/phone/btn_recall.png");
    private static final ResourceLocation TEX_BTN_ACCEPT  = new ResourceLocation("districtlife_phone", "textures/gui/phone/btn_accept.png");
    private static final ResourceLocation TEX_BTN_DECLINE = new ResourceLocation("districtlife_phone", "textures/gui/phone/btn_decline.png");
    private static final ResourceLocation TEX_BTN_HANGUP  = new ResourceLocation("districtlife_phone", "textures/gui/phone/btn_hangup.png");

    // -------------------------------------------------------------------------
    // Onglets IDLE
    // -------------------------------------------------------------------------

    private static final int TAB_DIAL    = 0;
    private static final int TAB_JOURNAL = 1;
    private static final int TAB_H       = 18;

    /** Libelles des touches du clavier numerique (3x4). */
    private static final String[] KEY_LABELS = { "1","2","3","4","5","6","7","8","9","*","0","\u232B" };

    private int    idleTab    = TAB_DIAL;
    private String dialDigits = "";  // chiffres bruts saisis (max 10)

    // scroll journal
    private int journalScroll = 0;
    private static final int JOURNAL_ITEM_H = 22;

    // -------------------------------------------------------------------------
    // Init / Tick
    // -------------------------------------------------------------------------

    @Override
    protected void onInit() {
        idleTab    = TAB_DIAL;
        dialDigits = "";
        journalScroll = 0;
    }

    @Override
    public void tick() {
        PhoneCallState.tickImpossible();
    }

    // -------------------------------------------------------------------------
    // Rendu principal
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        switch (PhoneCallState.getState()) {
            case CALLING:    renderCalling(stack, mouseX, mouseY);    break;
            case RINGING:    renderRinging(stack, mouseX, mouseY);    break;
            case INCALL:     renderInCall(stack, mouseX, mouseY);     break;
            case IMPOSSIBLE: renderImpossible(stack);                 break;
            default:         renderIdle(stack, mouseX, mouseY);       break;
        }
    }

    // -------------------------------------------------------------------------
    // IDLE — onglets Clavier / Journal
    // -------------------------------------------------------------------------

    private void renderIdle(MatrixStack stack, int mouseX, int mouseY) {
        drawTitleBar(stack, mouseX, mouseY);
        drawTabBar(stack, mouseX, mouseY);
        if (idleTab == TAB_DIAL) renderDial(stack, mouseX, mouseY);
        else                     renderJournal(stack, mouseX, mouseY);
    }

    /** Barre d'onglets sous la barre de titre. */
    private void drawTabBar(MatrixStack stack, int mouseX, int mouseY) {
        int tabY  = phoneY + 16;
        int tabW  = phoneWidth / 2;

        // Fond de la barre
        PhoneRenderHelper.drawTexture(stack, TEX_TAB_BG, phoneX, tabY, phoneWidth, TAB_H);

        for (int t = 0; t < 2; t++) {
            int tx      = phoneX + t * tabW;
            boolean sel = idleTab == t;
            boolean hov = isIn(mouseX, mouseY, tx, tabY, tabW, TAB_H);

            // Fond de l'onglet selon son etat
            ResourceLocation tabTex = sel ? TEX_TAB_SELECTED : (hov ? TEX_TAB_HOVER : TEX_TAB_NORMAL);
            PhoneRenderHelper.drawTexture(stack, tabTex, tx, tabY, tabW, TAB_H);

            // Soulignement de l'onglet actif
            if (sel) PhoneRenderHelper.drawTexture(stack, TEX_TAB_UNDERLINE, tx, tabY + TAB_H - 2, tabW, 2);

            // Separateur vertical entre onglets
            if (t == 0) PhoneRenderHelper.drawTexture(stack, TEX_TAB_SEPARATOR, tx + tabW - 1, tabY, 1, TAB_H);

            String label = (t == TAB_DIAL) ? "Clavier" : "Journal";
            int lw = PhoneFont.width(label);
            PhoneFont.draw(stack, label, tx + (tabW - lw) / 2.0F, tabY + (TAB_H - 8) / 2.0F,
                    sel ? 0xFFFFFFFF : 0xFF8888AA);
        }
    }

    // --- Clavier numerique ---

    private void renderDial(MatrixStack stack, int mouseX, int mouseY) {
        int contentY = phoneY + 16 + TAB_H;

        // Affichage du numero saisi
        int dispH = 20;
        PhoneRenderHelper.drawTexture(stack, TEX_NUMBER_DISPLAY, phoneX + 4, contentY + 4, phoneWidth - 8, dispH);
        String display = dialDigits.isEmpty() ? "\u2014\u2014\u2014" : formatPhoneDigits(dialDigits);
        int dw = PhoneFont.width(display);
        PhoneFont.draw(stack, display,
                phoneX + (phoneWidth - dw) / 2.0F, contentY + 4 + (dispH - 8) / 2.0F,
                dialDigits.isEmpty() ? 0xFF444466 : 0xFFEEEEEE);

        // Clavier 3x4
        int kbY  = contentY + dispH + 8;
        int cols = 3;
        int btnW = (phoneWidth - 10) / cols;
        int btnH = 16;
        int gapH = 3;

        for (int i = 0; i < KEY_LABELS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int bx  = phoneX + 4 + col * (btnW + 2);
            int by  = kbY + row * (btnH + gapH);

            boolean hov   = isIn(mouseX, mouseY, bx, by, btnW, btnH);
            boolean isDel = KEY_LABELS[i].equals("\u232B");
            PhoneRenderHelper.drawTexture(stack, isDel ? TEX_BTN_KEY_DEL : TEX_BTN_KEY, bx, by, btnW, btnH);
            if (hov) PhoneRenderHelper.fillRect(stack, bx, by, btnW, btnH, 0x33FFFFFF);

            String lbl = KEY_LABELS[i];
            int lw = PhoneFont.width(lbl);
            PhoneFont.draw(stack, lbl, bx + (btnW - lw) / 2.0F, by + (btnH - 8) / 2.0F, 0xFFCCCCDD);
        }

        // Bouton Appeler
        int callBtnY = kbY + 4 * (btnH + gapH) + 4;
        boolean canCall  = dialDigits.length() >= 1;
        boolean callHov  = canCall && isIn(mouseX, mouseY, phoneX + 6, callBtnY, phoneWidth - 12, 14);
        PhoneRenderHelper.drawTexture(stack, canCall ? TEX_BTN_CALL : TEX_BTN_CALL_DISABLED,
                phoneX + 6, callBtnY, phoneWidth - 12, 14);
        if (callHov) PhoneRenderHelper.fillRect(stack, phoneX + 6, callBtnY, phoneWidth - 12, 14, 0x33FFFFFF);
        String callLbl = "Appeler";
        int clw = PhoneFont.width(callLbl);
        PhoneFont.draw(stack, callLbl,
                phoneX + (phoneWidth - clw) / 2.0F, callBtnY + 3,
                canCall ? 0xFFFFFFFF : 0xFF446655);
    }

    // --- Journal d'appels ---

    private void renderJournal(MatrixStack stack, int mouseX, int mouseY) {
        int contentY = phoneY + 16 + TAB_H;
        int listH    = phoneHeight - 16 - TAB_H;
        int maxVis   = listH / JOURNAL_ITEM_H;

        List<CallLogEntry> log = CallLogClient.getEntries();

        if (log.isEmpty()) {
            String msg = "Aucun appel recent";
            PhoneFont.draw(stack, msg,
                    phoneX + (phoneWidth - PhoneFont.width(msg)) / 2.0F,
                    contentY + listH / 2.0F - 4,
                    0xFF555566);
            return;
        }

        for (int i = journalScroll; i < log.size(); i++) {
            int rel = i - journalScroll;
            if (rel >= maxVis) break;

            CallLogEntry e   = log.get(i);
            int itemY = contentY + rel * JOURNAL_ITEM_H;

            PhoneRenderHelper.drawTexture(stack,
                    rel % 2 == 0 ? TEX_JOURNAL_ROW_EVEN : TEX_JOURNAL_ROW_ODD,
                    phoneX, itemY, phoneWidth, JOURNAL_ITEM_H - 1);

            // Icone de type
            String icon;
            int    iconColor;
            String typeLabel;
            switch (e.type) {
                case OUTGOING: icon = "\u2192"; iconColor = 0xFF44CC66; typeLabel = "Sortant";  break;
                case INCOMING: icon = "\u2190"; iconColor = 0xFF4488FF; typeLabel = "Entrant";  break;
                default:       icon = "\u2197"; iconColor = 0xFFFF4444; typeLabel = "Manque";   break;
            }
            PhoneFont.draw(stack, icon, phoneX + 4, itemY + (JOURNAL_ITEM_H - 8) / 2.0F, iconColor);

            // Nom ou numero
            String name = e.displayName.isEmpty() || e.displayName.equals(e.number)
                    ? e.number : e.displayName;
            PhoneFont.draw(stack, name, phoneX + 14, itemY + 3, 0xFFEEEEEE);

            // Type + numero en petit
            stack.pushPose();
            stack.translate(phoneX + 14, itemY + 12, 0);
            stack.scale(0.75F, 0.75F, 1F);
            String sub = e.displayName.equals(e.number)
                    ? typeLabel : typeLabel + "  " + e.number;
            PhoneFont.draw(stack, sub, 0, 0, 0xFF666688);
            stack.popPose();

            // Bouton rappeler
            int rX = phoneX + phoneWidth - 22;
            int rY = itemY + (JOURNAL_ITEM_H - 12) / 2;
            boolean rHov = isIn(mouseX, mouseY, rX, rY, 18, 12);
            PhoneRenderHelper.drawTexture(stack, TEX_BTN_RECALL, rX, rY, 18, 12);
            if (rHov) PhoneRenderHelper.fillRect(stack, rX, rY, 18, 12, 0x33FFFFFF);
            PhoneFont.draw(stack, ">>", rX + 2, rY + 2, 0xFFFFFFFF);
        }

        // Scroll indicators
        if (journalScroll > 0)
            PhoneFont.draw(stack, "^", phoneX + phoneWidth / 2 - 2, contentY - 1, 0xFF555577);
        if (journalScroll + maxVis < log.size())
            PhoneFont.draw(stack, "v", phoneX + phoneWidth / 2 - 2,
                    contentY + maxVis * JOURNAL_ITEM_H, 0xFF555577);
    }

    // -------------------------------------------------------------------------
    // CALLING — appel sortant en sonnerie
    // -------------------------------------------------------------------------

    private void renderCalling(MatrixStack stack, int mouseX, int mouseY) {
        drawTitleBar(stack, mouseX, mouseY);

        String phone       = PhoneCallState.getOtherPhone();
        String displayName = resolveDisplayName(phone);
        int cy = phoneY + phoneHeight / 2 - 40;

        renderCenteredBig(stack, displayName, cy);
        if (!displayName.equals(phone))
            renderCenteredSmall(stack, phone, cy + 15, 0xFF777799);

        long ticks = Minecraft.getInstance().player.level.getGameTime();
        int dotsCount = (int) ((ticks / 12) % 4);
        StringBuilder sb = new StringBuilder("Appel en cours");
        for (int i = 0; i < dotsCount; i++) sb.append('.');
        renderCenteredSmall(stack, sb.toString(), cy + 32, 0xFF888899);

        renderHangupButton(stack, mouseX, mouseY);
    }

    // -------------------------------------------------------------------------
    // RINGING — appel entrant
    // -------------------------------------------------------------------------

    private void renderRinging(MatrixStack stack, int mouseX, int mouseY) {
        drawTitleBar(stack, mouseX, mouseY);

        String callerPhone = PhoneCallState.getOtherPhone();
        String displayName = resolveDisplayName(callerPhone);
        int cy = phoneY + phoneHeight / 2 - 45;

        renderCenteredSmall(stack, "Appel entrant", cy - 10, 0xFF888899);
        renderCenteredBig(stack, displayName, cy + 4);
        if (!displayName.equals(callerPhone))
            renderCenteredSmall(stack, callerPhone, cy + 20, 0xFF777799);

        int btnW     = 50;
        int btnH     = 16;
        int btnY     = phoneY + phoneHeight - 38;
        int acceptX  = phoneX + phoneWidth / 2 - btnW - 3;
        int declineX = phoneX + phoneWidth / 2 + 3;

        boolean acceptHov  = isIn(mouseX, mouseY, acceptX,  btnY, btnW, btnH);
        boolean declineHov = isIn(mouseX, mouseY, declineX, btnY, btnW, btnH);

        PhoneRenderHelper.drawTexture(stack, TEX_BTN_ACCEPT, acceptX, btnY, btnW, btnH);
        if (acceptHov)  PhoneRenderHelper.fillRect(stack, acceptX,  btnY, btnW, btnH, 0x33FFFFFF);
        PhoneRenderHelper.drawTexture(stack, TEX_BTN_DECLINE, declineX, btnY, btnW, btnH);
        if (declineHov) PhoneRenderHelper.fillRect(stack, declineX, btnY, btnW, btnH, 0x33FFFFFF);
        renderBtnLabel(stack, "Repondre", acceptX,  btnY, btnW, btnH);
        renderBtnLabel(stack, "Refuser",  declineX, btnY, btnW, btnH);
    }

    // -------------------------------------------------------------------------
    // INCALL — en communication
    // -------------------------------------------------------------------------

    private void renderInCall(MatrixStack stack, int mouseX, int mouseY) {
        drawTitleBar(stack, mouseX, mouseY);

        String phone       = PhoneCallState.getOtherPhone();
        String displayName = resolveDisplayName(phone);

        int cy = phoneY + phoneHeight / 2 - 40;
        renderCenteredBig(stack, displayName, cy);

        long now     = Minecraft.getInstance().player.level.getGameTime();
        long elapsed = Math.max(0, now - PhoneCallState.getCallStartTick());
        String timer = String.format("%02d:%02d", (elapsed / 20) / 60, (elapsed / 20) % 60);
        renderCenteredBig(stack, timer, cy + 18);
        renderCenteredSmall(stack, "En communication", cy + 34, 0xFF55BB66);

        renderHangupButton(stack, mouseX, mouseY);
    }

    // -------------------------------------------------------------------------
    // IMPOSSIBLE
    // -------------------------------------------------------------------------

    private void renderImpossible(MatrixStack stack) {
        drawTitleBar(stack, -1, -1);
        renderCenteredBig(stack, "Appel impossible", phoneY + phoneHeight / 2 - 10);
    }

    // -------------------------------------------------------------------------
    // Widgets communs
    // -------------------------------------------------------------------------

    private void renderHangupButton(MatrixStack stack, int mouseX, int mouseY) {
        int btnW = 66;
        int btnH = 16;
        int btnX = phoneX + (phoneWidth - btnW) / 2;
        int btnY = phoneY + phoneHeight - 28;
        boolean hov = isIn(mouseX, mouseY, btnX, btnY, btnW, btnH);
        PhoneRenderHelper.drawTexture(stack, TEX_BTN_HANGUP, btnX, btnY, btnW, btnH);
        if (hov) PhoneRenderHelper.fillRect(stack, btnX, btnY, btnW, btnH, 0x33FFFFFF);
        renderBtnLabel(stack, "Raccrocher", btnX, btnY, btnW, btnH);
    }

    private void renderBtnLabel(MatrixStack stack, String label, int bx, int by, int bw, int bh) {
        PhoneFont.draw(stack, label,
                bx + (bw - PhoneFont.width(label)) / 2.0F,
                by + (bh - 8) / 2.0F,
                0xFFFFFFFF);
    }

    private void renderCenteredBig(MatrixStack stack, String text, int y) {
        stack.pushPose();
        stack.translate(phoneX + phoneWidth / 2.0F, y, 0);
        stack.scale(1.1F, 1.1F, 1F);
        PhoneFont.draw(stack, text, -PhoneFont.width(text) / 2.0F, 0, 0xFFEEEEEE);
        stack.popPose();
    }

    private void renderCenteredSmall(MatrixStack stack, String text, int y, int color) {
        stack.pushPose();
        stack.translate(phoneX + phoneWidth / 2.0F, y, 0);
        stack.scale(0.8F, 0.8F, 1F);
        PhoneFont.draw(stack, text, -PhoneFont.width(text) / 2.0F, 0, color);
        stack.popPose();
    }

    // -------------------------------------------------------------------------
    // Interactions souris
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleBackButtonClick(mx, my)) return true;

        switch (PhoneCallState.getState()) {
            case IDLE:    return handleIdleClick(mx, my);
            case CALLING: return handleHangupClick(mx, my);
            case RINGING: return handleRingingClick(mx, my);
            case INCALL:  return handleHangupClick(mx, my);
        }
        return false;
    }

    private boolean handleIdleClick(double mx, double my) {
        // Onglets
        int tabY = phoneY + 16;
        int tabW = phoneWidth / 2;
        if (isIn(mx, my, phoneX, tabY, tabW, TAB_H)) {
            idleTab = TAB_DIAL; return true;
        }
        if (isIn(mx, my, phoneX + tabW, tabY, tabW, TAB_H)) {
            idleTab = TAB_JOURNAL; return true;
        }

        if (idleTab == TAB_DIAL)    return handleDialClick(mx, my);
        else                        return handleJournalClick(mx, my);
    }

    private boolean handleDialClick(double mx, double my) {
        int contentY = phoneY + 16 + TAB_H;
        int kbY      = contentY + 20 + 8;
        int cols     = 3;
        int btnW     = (phoneWidth - 10) / cols;
        int btnH     = 16;
        int gapH     = 3;

        // Touches du clavier
        for (int i = 0; i < KEY_LABELS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int bx  = phoneX + 4 + col * (btnW + 2);
            int by  = kbY + row * (btnH + gapH);
            if (isIn(mx, my, bx, by, btnW, btnH)) {
                pressKey(KEY_LABELS[i]);
                return true;
            }
        }

        // Bouton Appeler
        int callBtnY = kbY + 4 * (btnH + gapH) + 4;
        if (dialDigits.length() >= 1 && isIn(mx, my, phoneX + 6, callBtnY, phoneWidth - 12, 14)) {
            startCall(formatPhoneDigits(dialDigits), "");
            return true;
        }
        return false;
    }

    private boolean handleJournalClick(double mx, double my) {
        int contentY = phoneY + 16 + TAB_H;
        int maxVis   = (phoneHeight - 16 - TAB_H) / JOURNAL_ITEM_H;
        List<CallLogEntry> log = CallLogClient.getEntries();

        for (int i = journalScroll; i < log.size(); i++) {
            int rel  = i - journalScroll;
            if (rel >= maxVis) break;
            int itemY = contentY + rel * JOURNAL_ITEM_H;

            int rX = phoneX + phoneWidth - 22;
            int rY = itemY + (JOURNAL_ITEM_H - 12) / 2;
            if (isIn(mx, my, rX, rY, 18, 12)) {
                CallLogEntry e = log.get(i);
                startCall(e.number, e.displayName);
                return true;
            }
        }
        return false;
    }

    private boolean handleRingingClick(double mx, double my) {
        int btnW     = 50;
        int btnH     = 16;
        int btnY     = phoneY + phoneHeight - 38;
        int acceptX  = phoneX + phoneWidth / 2 - btnW - 3;
        int declineX = phoneX + phoneWidth / 2 + 3;

        if (isIn(mx, my, acceptX,  btnY, btnW, btnH)) { acceptCall();  return true; }
        if (isIn(mx, my, declineX, btnY, btnW, btnH)) { declineCall(); return true; }
        return false;
    }

    private boolean handleHangupClick(double mx, double my) {
        int btnW = 66;
        int btnH = 16;
        int btnX = phoneX + (phoneWidth - btnW) / 2;
        int btnY = phoneY + phoneHeight - 28;
        if (isIn(mx, my, btnX, btnY, btnW, btnH)) { hangup(); return true; }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (PhoneCallState.isIdle() && idleTab == TAB_JOURNAL) {
            int maxVis   = (phoneHeight - 16 - TAB_H) / JOURNAL_ITEM_H;
            int maxScroll = Math.max(0, CallLogClient.getEntries().size() - maxVis);
            journalScroll = (int) Math.max(0, Math.min(maxScroll, journalScroll - delta));
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Actions clavier numerique
    // -------------------------------------------------------------------------

    private void pressKey(String key) {
        if (key.equals("\u232B")) {
            if (!dialDigits.isEmpty())
                dialDigits = dialDigits.substring(0, dialDigits.length() - 1);
        } else if (Character.isDigit(key.charAt(0))) {
            if (dialDigits.length() < 10) dialDigits += key;
        }
        // '*' ignore
    }

    // -------------------------------------------------------------------------
    // Actions reseau
    // -------------------------------------------------------------------------

    /**
     * Demarre un appel vers le numero donne.
     * displayName peut etre vide si inconnu (sera resolu localement depuis les contacts).
     */
    public void startCall(String targetPhone, String displayName) {
        String rawDigits = targetPhone.replaceAll("[^0-9]", "");
        if (rawDigits.isEmpty()) return;
        // Conserve le format "06 XX XX XX XX" (avec espaces) coherent avec le NBT des telephones
        String formattedNumber = formatPhoneDigits(rawDigits);

        String name = (displayName == null || displayName.isEmpty())
                ? resolveDisplayName(formattedNumber) : displayName;

        long tick = Minecraft.getInstance().player.level.getGameTime();
        CallLogClient.add(new CallLogEntry(formattedNumber, name, CallLogEntry.Type.OUTGOING, tick));

        PhoneCallState.setCalling(formattedNumber, name);
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignal(CallSignal.CALL, phoneScreen.getPhoneNumber(), formattedNumber));

        dialDigits = "";
    }

    private void acceptCall() {
        String callerPhone = PhoneCallState.getOtherPhone();
        String callerName  = resolveDisplayName(callerPhone);

        long tick = Minecraft.getInstance().player.level.getGameTime();
        CallLogClient.add(new CallLogEntry(callerPhone, callerName, CallLogEntry.Type.INCOMING, tick));

        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignal(CallSignal.ACCEPT, phoneScreen.getPhoneNumber(), callerPhone));
    }

    private void declineCall() {
        String callerPhone = PhoneCallState.getOtherPhone();
        com.districtlife.phone.network.PhoneClientHandler.stopRingSound();
        PhoneCallState.reset();
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignal(CallSignal.DECLINE, phoneScreen.getPhoneNumber(), callerPhone));
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.playSound(ModSounds.PHONE_HANGUP.get(), 0.8f, 1.0f);
    }

    private void hangup() {
        if (PhoneCallState.getState() == CallState.RINGING) {
            String phone = PhoneCallState.getOtherPhone();
            long tick = Minecraft.getInstance().player.level.getGameTime();
            CallLogClient.add(new CallLogEntry(phone, resolveDisplayName(phone),
                    CallLogEntry.Type.MISSED, tick));
        }
        com.districtlife.phone.network.PhoneClientHandler.stopRingSound();
        PhoneCallState.reset();
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignal(CallSignal.HANGUP, phoneScreen.getPhoneNumber(), ""));
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.playSound(ModSounds.PHONE_HANGUP.get(), 0.8f, 1.0f);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String resolveDisplayName(String phone) {
        for (Contact c : PhoneData.getContacts(getPhoneStack())) {
            if (c.getPhoneNumber().equals(phone)) return c.getPseudo();
        }
        return phone;
    }

    /** Formate les chiffres bruts en groupes de 2 (ex: "0612345678" → "06 12 34 56 78"). */
    private static String formatPhoneDigits(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 2 == 0) sb.append(' ');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    private boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public String getTitle() {
        switch (PhoneCallState.getState()) {
            case CALLING:    return "Appel sortant";
            case RINGING:    return "Appel entrant";
            case INCALL:     return "En communication";
            case IMPOSSIBLE: return "Telephone";
            default:         return "Telephone";
        }
    }

    @Override
    public void onBack() {
        CallState s = PhoneCallState.getState();
        if (s == CallState.IDLE || s == CallState.IMPOSSIBLE) {
            super.onBack();
        }
    }
}
