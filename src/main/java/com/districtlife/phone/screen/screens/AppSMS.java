package com.districtlife.phone.screen.screens;

import com.districtlife.phone.capability.Contact;
import com.districtlife.phone.capability.Conversation;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketSendSMS;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AppSMS extends AbstractPhoneApp {

    // --- Etats ---
    private static final int STATE_LIST   = 0; // liste des conversations
    private static final int STATE_NEW    = 1; // choix du destinataire
    private static final int STATE_CHAT   = 2; // conversation ouverte

    // Layout
    private static final int TITLE_H    = 16;
    private static final int CONV_ITEM_H = 28;
    private static final int INPUT_H    = 20;
    private static final int BUBBLE_MAX_W = 110;

    private int state = STATE_LIST;

    // Conversation active
    private String   chatPhone       = "";   // numero de l'autre partie
    private int      chatScroll      = 0;    // offset en pixels depuis le bas
    private int      listScroll      = 0;

    // Widget saisie de message
    private TextFieldWidget inputField;

    // Scroll de la liste des contacts pour STATE_NEW
    private int newContactScroll = 0;
    private static final int CONTACT_ITEM_H = 22;

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void onInit() {
        inputField = new TextFieldWidget(getFont(),
                phoneX + 4,
                phoneY + phoneHeight - INPUT_H + 4,
                phoneWidth - 24, 12,
                new StringTextComponent(""));
        inputField.setMaxLength(256);
        inputField.setTextColor(0xFFFFFFFF);
        inputField.setBordered(false);

        state = STATE_LIST;
        listScroll = 0;
        chatScroll = 0;
        newContactScroll = 0;
    }

    @Override
    public void tick() {
        if (inputField != null) inputField.tick();
    }

    // -------------------------------------------------------------------------
    // Rendu principal
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        switch (state) {
            case STATE_LIST: renderList(stack, mouseX, mouseY, partialTicks); break;
            case STATE_NEW:  renderNew(stack, mouseX, mouseY, partialTicks);  break;
            case STATE_CHAT: renderChat(stack, mouseX, mouseY, partialTicks); break;
        }
    }

    // -------------------------------------------------------------------------
    // STATE_LIST — liste des conversations
    // -------------------------------------------------------------------------

    private void renderList(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        drawTitleBar(stack, mouseX, mouseY);

        List<Conversation> convs = getConversations();
        int listTop  = phoneY + TITLE_H;
        int listH    = phoneHeight - TITLE_H - 18; // 18 = bouton "Nouveau"
        int maxVisible = listH / CONV_ITEM_H;

        if (convs.isEmpty()) {
            String msg = "Aucun message";
            int mw = getFont().width(msg);
            getFont().draw(stack, msg,
                    phoneX + (phoneWidth - mw) / 2.0F,
                    phoneY + phoneHeight / 2.0F - 4,
                    0xFF555566);
        } else {
            for (int i = listScroll; i < convs.size(); i++) {
                int rel = i - listScroll;
                if (rel >= maxVisible) break;

                Conversation conv = convs.get(i);
                int itemY = listTop + rel * CONV_ITEM_H;

                // Fond de ligne
                boolean hov = isIn(mouseX, mouseY, phoneX, itemY, phoneWidth, CONV_ITEM_H - 1);
                PhoneRenderHelper.fillRect(stack, phoneX, itemY, phoneWidth, CONV_ITEM_H - 1,
                        hov ? 0x33FFFFFF : (rel % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF));

                // Indicateur non lu
                if (hasUnreadInConv(conv)) {
                    PhoneRenderHelper.fillRect(stack, phoneX + 2, itemY + 10, 5, 5, 0xFFFF3333);
                }

                // Nom du contact (ou numero)
                String displayName = resolveDisplayName(conv.getContactPhoneNumber());
                getFont().draw(stack, displayName, phoneX + 10, itemY + 3, 0xFFEEEEEE);

                // Apercu dernier message
                Conversation.Message last = conv.getLastMessage();
                if (last != null) {
                    String preview = (last.isOutgoing() ? "Vous: " : "") + last.getText();
                    if (getFont().width(preview) > phoneWidth - 14) {
                        while (getFont().width(preview + "...") > phoneWidth - 14 && preview.length() > 0) {
                            preview = preview.substring(0, preview.length() - 1);
                        }
                        preview += "...";
                    }
                    stack.pushPose();
                    stack.translate(phoneX + 10, itemY + 14, 0);
                    stack.scale(0.75F, 0.75F, 1F);
                    getFont().draw(stack, preview, 0, 0, 0xFF777799);
                    stack.popPose();
                }

                // Chevron
                getFont().draw(stack, ">", phoneX + phoneWidth - 10, itemY + 10, 0xFF444466);
            }

            // Indicateurs scroll
            if (listScroll > 0)
                getFont().draw(stack, "^", phoneX + phoneWidth / 2 - 2, listTop, 0xFF555577);
            if (listScroll + maxVisible < convs.size())
                getFont().draw(stack, "v", phoneX + phoneWidth / 2 - 2,
                        listTop + maxVisible * CONV_ITEM_H, 0xFF555577);
        }

        // Bouton "Nouveau"
        int btnY = phoneY + phoneHeight - 16;
        boolean btnHov = isIn(mouseX, mouseY, phoneX + phoneWidth / 2 - 22, btnY, 44, 13);
        PhoneRenderHelper.fillRect(stack, phoneX + phoneWidth / 2 - 22, btnY, 44, 13,
                btnHov ? 0xFF3355CC : 0xFF1A2E88);
        String lbl = "Nouveau";
        getFont().draw(stack, lbl, phoneX + (phoneWidth - getFont().width(lbl)) / 2.0F, btnY + 3, 0xFFFFFFFF);
    }

    // -------------------------------------------------------------------------
    // STATE_NEW — choix du destinataire
    // -------------------------------------------------------------------------

    private void renderNew(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        drawTitleBar(stack, mouseX, mouseY);

        List<Contact> contacts = getContacts();
        int top = phoneY + TITLE_H + 2;

        if (contacts.isEmpty()) {
            String msg = "Aucun contact enregistre";
            int mw = getFont().width(msg);
            getFont().draw(stack, msg,
                    phoneX + (phoneWidth - mw) / 2.0F,
                    phoneY + phoneHeight / 2.0F - 4,
                    0xFF555566);
            return;
        }

        int maxVis = (phoneHeight - TITLE_H - 2) / CONTACT_ITEM_H;
        for (int i = newContactScroll; i < contacts.size(); i++) {
            int rel = i - newContactScroll;
            if (rel >= maxVis) break;
            Contact c = contacts.get(i);
            int itemY = top + rel * CONTACT_ITEM_H;

            boolean hov = isIn(mouseX, mouseY, phoneX, itemY, phoneWidth, CONTACT_ITEM_H - 1);
            PhoneRenderHelper.fillRect(stack, phoneX, itemY, phoneWidth, CONTACT_ITEM_H - 1,
                    hov ? 0x33FFFFFF : 0x11FFFFFF);

            getFont().draw(stack, c.getPseudo(), phoneX + 8, itemY + 3, 0xFFEEEEEE);

            String num = c.getPhoneNumber().isEmpty() ? "Pas de numero" : c.getPhoneNumber();
            stack.pushPose();
            stack.translate(phoneX + 8, itemY + 13, 0);
            stack.scale(0.75F, 0.75F, 1F);
            getFont().draw(stack, num, 0, 0, 0xFF777799);
            stack.popPose();
        }
    }

    // -------------------------------------------------------------------------
    // STATE_CHAT — vue conversation
    // -------------------------------------------------------------------------

    private void renderChat(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        // Titre = nom du contact ou numero
        drawTitleBar(stack, mouseX, mouseY);

        // Zone de messages
        int chatTop = phoneY + TITLE_H;
        int chatH   = phoneHeight - TITLE_H - INPUT_H;

        // Clip en remplissant le fond
        PhoneRenderHelper.fillRect(stack, phoneX, chatTop, phoneWidth, chatH, 0x22000000);

        // Recupere et calcule les bulles
        Conversation conv = getConversation(chatPhone);
        if (conv != null && !conv.getMessages().isEmpty()) {
            List<Bubble> bubbles = buildBubbles(conv.getMessages());

            // Hauteur totale de toutes les bulles
            int totalH = 0;
            for (Bubble b : bubbles) totalH += b.totalHeight + 4;

            // chatScroll = pixels depuis le bas (0 = tout en bas)
            int maxScroll = Math.max(0, totalH - chatH);
            chatScroll = Math.min(chatScroll, maxScroll);

            // Dessine depuis le bas
            int drawY = chatTop + chatH - 4; // on remonte depuis ici
            // On dessine toutes les bulles de bas en haut
            for (int i = bubbles.size() - 1; i >= 0; i--) {
                Bubble b = bubbles.get(i);
                drawY -= b.totalHeight + 4;
                int renderY = drawY + chatScroll; // decale par scroll
                if (renderY + b.totalHeight < chatTop) break; // en dehors de la zone
                if (renderY < chatTop + chatH) {
                    renderBubble(stack, b, renderY, chatTop, chatTop + chatH);
                }
            }
        }

        // Zone de saisie
        int inputY = phoneY + phoneHeight - INPUT_H;
        PhoneRenderHelper.fillRect(stack, phoneX, inputY, phoneWidth, INPUT_H, 0xFF0D0D20);
        PhoneRenderHelper.fillRect(stack, phoneX, inputY, phoneWidth, 1, 0xFF222244);

        // Champ de saisie
        inputField.y = inputY + 4;
        inputField.x = phoneX + 4;
        inputField.setWidth(phoneWidth - 24);
        inputField.render(stack, mouseX, mouseY, partialTicks);

        // Bouton envoi ">"
        boolean sendHov = isIn(mouseX, mouseY, phoneX + phoneWidth - 18, inputY + 3, 14, 14);
        PhoneRenderHelper.fillRect(stack, phoneX + phoneWidth - 18, inputY + 3, 14, 14,
                sendHov ? 0xFF3355CC : 0xFF1A2E88);
        getFont().draw(stack, ">", phoneX + phoneWidth - 13, inputY + 6, 0xFFFFFFFF);
    }

    private void renderBubble(MatrixStack stack, Bubble b, int y, int clipTop, int clipBot) {
        if (y + b.totalHeight < clipTop || y > clipBot) return;

        int bubbleW = b.maxLineW + 8;
        int bubbleX = b.outgoing
                ? phoneX + phoneWidth - bubbleW - 4
                : phoneX + 4;
        int bgColor = b.outgoing ? 0xFF1A4D7A : 0xFF2D2D4A;

        // Fond bulle
        PhoneRenderHelper.fillRect(stack, bubbleX, y, bubbleW, b.totalHeight, bgColor);

        // Texte
        int lineY = y + 3;
        for (String line : b.lines) {
            if (lineY >= clipTop && lineY < clipBot) {
                getFont().draw(stack, line, bubbleX + 4, lineY, 0xFFFFFFFF);
            }
            lineY += 9;
        }
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleBackButtonClick(mx, my)) return true;

        switch (state) {
            case STATE_LIST: return handleListClick(mx, my);
            case STATE_NEW:  return handleNewClick(mx, my);
            case STATE_CHAT: return handleChatClick(mx, my, button);
        }
        return false;
    }

    private boolean handleListClick(double mx, double my) {
        // Bouton Nouveau
        int btnY = phoneY + phoneHeight - 16;
        if (isIn(mx, my, phoneX + phoneWidth / 2 - 22, btnY, 44, 13)) {
            state = STATE_NEW;
            newContactScroll = 0;
            return true;
        }

        // Clic sur une conversation
        List<Conversation> convs = getConversations();
        int top = phoneY + TITLE_H;
        int listH = phoneHeight - TITLE_H - 18;
        int maxVis = listH / CONV_ITEM_H;
        for (int i = listScroll; i < convs.size(); i++) {
            int rel = i - listScroll;
            if (rel >= maxVis) break;
            int itemY = top + rel * CONV_ITEM_H;
            if (isIn(mx, my, phoneX, itemY, phoneWidth, CONV_ITEM_H - 1)) {
                openChat(convs.get(i).getContactPhoneNumber());
                return true;
            }
        }
        return false;
    }

    private boolean handleNewClick(double mx, double my) {
        List<Contact> contacts = getContacts();
        int top = phoneY + TITLE_H + 2;
        int maxVis = (phoneHeight - TITLE_H - 2) / CONTACT_ITEM_H;
        for (int i = newContactScroll; i < contacts.size(); i++) {
            int rel = i - newContactScroll;
            if (rel >= maxVis) break;
            Contact c = contacts.get(i);
            if (!c.getPhoneNumber().isEmpty()
                    && isIn(mx, my, phoneX, top + rel * CONTACT_ITEM_H, phoneWidth, CONTACT_ITEM_H - 1)) {
                openChat(c.getPhoneNumber());
                return true;
            }
        }
        return false;
    }

    private boolean handleChatClick(double mx, double my, int button) {
        // Focus champ de saisie
        if (inputField.mouseClicked(mx, my, button)) return true;

        // Bouton envoi
        int inputY = phoneY + phoneHeight - INPUT_H;
        if (isIn(mx, my, phoneX + phoneWidth - 18, inputY + 3, 14, 14)) {
            sendMessage();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        switch (state) {
            case STATE_LIST: {
                int maxVis = (phoneHeight - TITLE_H - 18) / CONV_ITEM_H;
                int max = Math.max(0, getConversations().size() - maxVis);
                listScroll = (int) Math.max(0, Math.min(max, listScroll - delta));
                return true;
            }
            case STATE_NEW: {
                int maxVis = (phoneHeight - TITLE_H - 2) / CONTACT_ITEM_H;
                int max = Math.max(0, getContacts().size() - maxVis);
                newContactScroll = (int) Math.max(0, Math.min(max, newContactScroll - delta));
                return true;
            }
            case STATE_CHAT: {
                chatScroll = (int) Math.max(0, chatScroll + delta * 10);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (state == STATE_CHAT) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendMessage();
                return true;
            }
            if (inputField.isFocused() && inputField.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (state == STATE_CHAT && inputField.isFocused()) {
            return inputField.charTyped(c, modifiers);
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void openChat(String phoneNumber) {
        chatPhone  = phoneNumber;
        chatScroll = 0;
        state      = STATE_CHAT;
        inputField.setValue("");
        // Efface le flag non-lu directement sur l'item
        PhoneData.setHasUnreadSMS(getPhoneStack(), false);
    }

    private void sendMessage() {
        String text = inputField.getValue().trim();
        if (text.isEmpty() || chatPhone.isEmpty()) return;
        PacketHandler.CHANNEL.sendToServer(
                new PacketSendSMS(phoneScreen.getPhoneNumber(), chatPhone, text));
        inputField.setValue("");
        chatScroll = 0;
    }

    private List<Conversation> getConversations() {
        return PhoneData.getConversations(getPhoneStack());
    }

    private List<Contact> getContacts() {
        return PhoneData.getContacts(getPhoneStack());
    }

    private Conversation getConversation(String phone) {
        return PhoneData.findConversation(getPhoneStack(), phone);
    }

    /** Retourne le pseudo du contact ayant ce numero, ou le numero lui-meme si inconnu. */
    private String resolveDisplayName(String phone) {
        List<Contact> contacts = getContacts();
        for (Contact c : contacts) {
            if (c.getPhoneNumber().equals(phone)) return c.getPseudo();
        }
        return phone;
    }

    private boolean hasUnreadInConv(Conversation conv) {
        Conversation.Message last = conv.getLastMessage();
        return last != null && !last.isOutgoing()
                && PhoneData.hasUnreadSMS(getPhoneStack());
    }

    // -------------------------------------------------------------------------
    // Rendu des bulles
    // -------------------------------------------------------------------------

    private static final class Bubble {
        List<String> lines;
        int maxLineW;
        int totalHeight;
        boolean outgoing;

        Bubble(List<String> lines, int maxLineW, boolean outgoing, int lineHeight) {
            this.lines       = lines;
            this.maxLineW    = maxLineW;
            this.outgoing    = outgoing;
            this.totalHeight = lines.size() * lineHeight + 6;
        }
    }

    private List<Bubble> buildBubbles(List<Conversation.Message> messages) {
        List<Bubble> result = new ArrayList<>();
        for (Conversation.Message msg : messages) {
            List<String> lines = wrapText(msg.getText(), BUBBLE_MAX_W - 8);
            int maxW = 0;
            for (String l : lines) maxW = Math.max(maxW, getFont().width(l));
            result.add(new Bubble(lines, maxW, msg.isOutgoing(), 9));
        }
        return result;
    }

    private List<String> wrapText(String text, int maxPx) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ", -1);
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (getFont().width(test) <= maxPx) {
                cur = new StringBuilder(test);
            } else {
                if (cur.length() > 0) lines.add(cur.toString());
                cur = new StringBuilder(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines.isEmpty() ? Collections.singletonList("") : lines;
    }

    private boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public String getTitle() {
        switch (state) {
            case STATE_NEW:  return "Nouveau message";
            case STATE_CHAT: return resolveDisplayName(chatPhone);
            default:         return "SMS";
        }
    }

    @Override
    public void onBack() {
        if (state == STATE_CHAT || state == STATE_NEW) {
            state = STATE_LIST;
            chatPhone = "";
        } else {
            super.onBack();
        }
    }
}
