package com.districtlife.phone.screen.screens;

import com.districtlife.phone.call.CallSignal;
import com.districtlife.phone.call.PhoneCallState;
import com.districtlife.phone.call.PhoneCallState.CallState;
import com.districtlife.phone.capability.Contact;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.network.PacketCallSignal;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AppPhone extends AbstractPhoneApp {

    private static final int CONTACT_ITEM_H = 26;

    private List<Contact> callableContacts = new ArrayList<>();
    private int contactScroll = 0;

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void onInit() {
        contactScroll = 0;
        refreshContacts();
    }

    @Override
    public void tick() {
        refreshContacts();
        PhoneCallState.tickImpossible();
    }

    private void refreshContacts() {
        callableContacts = new ArrayList<>();
        for (Contact c : PhoneData.getContacts(getPhoneStack())) {
            if (!c.getPhoneNumber().isEmpty()) {
                callableContacts.add(c);
            }
        }
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
            default:         renderDial(stack, mouseX, mouseY);       break;
        }
    }

    // -------------------------------------------------------------------------
    // IDLE — liste des contacts appelables
    // -------------------------------------------------------------------------

    private void renderDial(MatrixStack stack, int mouseX, int mouseY) {
        drawTitleBar(stack, mouseX, mouseY);

        int top    = phoneY + 16 + 2;
        int maxVis = (phoneHeight - 16) / CONTACT_ITEM_H;

        if (callableContacts.isEmpty()) {
            String msg = "Aucun contact avec numero";
            getFont().draw(stack, msg,
                    phoneX + (phoneWidth - getFont().width(msg)) / 2.0F,
                    phoneY + phoneHeight / 2.0F - 4,
                    0xFF555566);
            return;
        }

        for (int i = contactScroll; i < callableContacts.size(); i++) {
            int rel = i - contactScroll;
            if (rel >= maxVis) break;

            Contact c = callableContacts.get(i);
            int itemY = top + rel * CONTACT_ITEM_H;

            boolean rowHov = isIn(mouseX, mouseY, phoneX, itemY, phoneWidth, CONTACT_ITEM_H - 1);
            PhoneRenderHelper.fillRect(stack, phoneX, itemY, phoneWidth, CONTACT_ITEM_H - 1,
                    rowHov ? 0x33FFFFFF : (rel % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF));

            getFont().draw(stack, c.getPseudo(), phoneX + 8, itemY + 4, 0xFFEEEEEE);

            stack.pushPose();
            stack.translate(phoneX + 8, itemY + 14, 0);
            stack.scale(0.75F, 0.75F, 1F);
            getFont().draw(stack, c.getPhoneNumber(), 0, 0, 0xFF777799);
            stack.popPose();

            // Bouton appel (vert)
            int btnX = phoneX + phoneWidth - 22;
            int btnY = itemY + (CONTACT_ITEM_H - 14) / 2;
            boolean btnHov = isIn(mouseX, mouseY, btnX, btnY, 18, 14);
            PhoneRenderHelper.fillRect(stack, btnX, btnY, 18, 14,
                    btnHov ? 0xFF33CC55 : 0xFF116633);
            getFont().draw(stack, ">>>", btnX + 1, btnY + 3, 0xFFFFFFFF);
        }

        if (contactScroll > 0)
            getFont().draw(stack, "^", phoneX + phoneWidth / 2 - 2, top - 1, 0xFF555577);
        if (contactScroll + maxVis < callableContacts.size())
            getFont().draw(stack, "v", phoneX + phoneWidth / 2 - 2,
                    top + maxVis * CONTACT_ITEM_H, 0xFF555577);
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
        if (!displayName.equals(phone)) {
            renderCenteredSmall(stack, phone, cy + 15, 0xFF777799);
        }

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

        String callerPhone  = PhoneCallState.getOtherPhone();
        String displayName  = resolveDisplayName(callerPhone);

        int cy = phoneY + phoneHeight / 2 - 45;

        renderCenteredSmall(stack, "Appel entrant", cy - 10, 0xFF888899);
        renderCenteredBig(stack, displayName, cy + 4);
        if (!displayName.equals(callerPhone)) {
            renderCenteredSmall(stack, callerPhone, cy + 20, 0xFF777799);
        }

        // Boutons Repondre / Refuser
        int btnW = 50;
        int btnH = 16;
        int btnY = phoneY + phoneHeight - 38;
        int acceptX  = phoneX + phoneWidth / 2 - btnW - 3;
        int declineX = phoneX + phoneWidth / 2 + 3;

        boolean acceptHov  = isIn(mouseX, mouseY, acceptX,  btnY, btnW, btnH);
        boolean declineHov = isIn(mouseX, mouseY, declineX, btnY, btnW, btnH);

        PhoneRenderHelper.fillRect(stack, acceptX, btnY, btnW, btnH,
                acceptHov ? 0xFF33CC55 : 0xFF116633);
        renderBtnLabel(stack, "Repondre", acceptX, btnY, btnW, btnH);

        PhoneRenderHelper.fillRect(stack, declineX, btnY, btnW, btnH,
                declineHov ? 0xFFCC3333 : 0xFF882222);
        renderBtnLabel(stack, "Refuser", declineX, btnY, btnW, btnH);
    }

    // -------------------------------------------------------------------------
    // INCALL — en communication
    // -------------------------------------------------------------------------

    private void renderInCall(MatrixStack stack, int mouseX, int mouseY) {
        drawTitleBar(stack, mouseX, mouseY);

        String phone       = PhoneCallState.getOtherPhone();
        String displayName = resolveDisplayName(phone);
        if (displayName.equals(phone)) displayName = PhoneCallState.getOtherMcName();

        int cy = phoneY + phoneHeight / 2 - 40;

        renderCenteredBig(stack, displayName, cy);

        long now     = Minecraft.getInstance().player.level.getGameTime();
        long elapsed = Math.max(0, now - PhoneCallState.getCallStartTick());
        long sec     = (elapsed / 20) % 60;
        long min     = (elapsed / 20) / 60;
        String timer = String.format("%02d:%02d", min, sec);
        renderCenteredBig(stack, timer, cy + 18);
        renderCenteredSmall(stack, "En communication", cy + 34, 0xFF55BB66);

        renderHangupButton(stack, mouseX, mouseY);
    }

    // -------------------------------------------------------------------------
    // IMPOSSIBLE — appel non autorise (meme joueur ou cible deconnectee)
    // -------------------------------------------------------------------------

    private void renderImpossible(MatrixStack stack) {
        drawTitleBar(stack, -1, -1);
        int cy = phoneY + phoneHeight / 2 - 10;
        renderCenteredBig(stack, "Appel impossible", cy);
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
        PhoneRenderHelper.fillRect(stack, btnX, btnY, btnW, btnH,
                hov ? 0xFFDD3333 : 0xFF992222);
        renderBtnLabel(stack, "Raccrocher", btnX, btnY, btnW, btnH);
    }

    private void renderBtnLabel(MatrixStack stack, String label, int btnX, int btnY, int btnW, int btnH) {
        getFont().draw(stack, label,
                btnX + (btnW - getFont().width(label)) / 2.0F,
                btnY + (btnH - 8) / 2.0F,
                0xFFFFFFFF);
    }

    private void renderCenteredBig(MatrixStack stack, String text, int y) {
        stack.pushPose();
        stack.translate(phoneX + phoneWidth / 2.0F, y, 0);
        stack.scale(1.1F, 1.1F, 1F);
        getFont().draw(stack, text, -getFont().width(text) / 2.0F, 0, 0xFFEEEEEE);
        stack.popPose();
    }

    private void renderCenteredSmall(MatrixStack stack, String text, int y, int color) {
        stack.pushPose();
        stack.translate(phoneX + phoneWidth / 2.0F, y, 0);
        stack.scale(0.8F, 0.8F, 1F);
        getFont().draw(stack, text, -getFont().width(text) / 2.0F, 0, color);
        stack.popPose();
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleBackButtonClick(mx, my)) return true;

        switch (PhoneCallState.getState()) {
            case IDLE:    return handleDialClick(mx, my);
            case CALLING: return handleHangupClick(mx, my);
            case RINGING: return handleRingingClick(mx, my);
            case INCALL:  return handleHangupClick(mx, my);
        }
        return false;
    }

    private boolean handleDialClick(double mx, double my) {
        int top    = phoneY + 16 + 2;
        int maxVis = (phoneHeight - 16) / CONTACT_ITEM_H;

        for (int i = contactScroll; i < callableContacts.size(); i++) {
            int rel = i - contactScroll;
            if (rel >= maxVis) break;
            int itemY = top + rel * CONTACT_ITEM_H;
            int btnX  = phoneX + phoneWidth - 22;
            int btnY  = itemY + (CONTACT_ITEM_H - 14) / 2;
            if (isIn(mx, my, btnX, btnY, 18, 14)) {
                Contact c = callableContacts.get(i);
                startCall(c.getPhoneNumber(), c.getPseudo());
                return true;
            }
        }
        return false;
    }

    private boolean handleRingingClick(double mx, double my) {
        int btnW = 50;
        int btnH = 16;
        int btnY = phoneY + phoneHeight - 38;
        int acceptX  = phoneX + phoneWidth / 2 - btnW - 3;
        int declineX = phoneX + phoneWidth / 2 + 3;

        if (isIn(mx, my, acceptX, btnY, btnW, btnH)) { acceptCall();  return true; }
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
        if (PhoneCallState.isIdle()) {
            int maxVis = (phoneHeight - 16) / CONTACT_ITEM_H;
            int max = Math.max(0, callableContacts.size() - maxVis);
            contactScroll = (int) Math.max(0, Math.min(max, contactScroll - delta));
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Actions reseau
    // -------------------------------------------------------------------------

    private void startCall(String targetPhone, String displayName) {
        PhoneCallState.setCalling(targetPhone, displayName);
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignal(CallSignal.CALL, phoneScreen.getPhoneNumber(), targetPhone));
    }

    private void acceptCall() {
        String callerPhone = PhoneCallState.getOtherPhone();
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignal(CallSignal.ACCEPT, phoneScreen.getPhoneNumber(), callerPhone));
    }

    private void declineCall() {
        String callerPhone = PhoneCallState.getOtherPhone();
        PhoneCallState.reset();
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignal(CallSignal.DECLINE, phoneScreen.getPhoneNumber(), callerPhone));
    }

    private void hangup() {
        PhoneCallState.reset();
        PacketHandler.CHANNEL.sendToServer(
                new PacketCallSignal(CallSignal.HANGUP, phoneScreen.getPhoneNumber(), ""));
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
        // Bloque la navigation retour pendant un appel actif
        CallState s = PhoneCallState.getState();
        if (s == CallState.IDLE || s == CallState.IMPOSSIBLE) {
            super.onBack();
        }
    }
}
