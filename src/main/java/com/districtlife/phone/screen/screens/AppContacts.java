package com.districtlife.phone.screen.screens;

import com.districtlife.phone.capability.Contact;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.network.PacketAddContact;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketRemoveContact;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;
import com.districtlife.phone.util.PhoneFont;

@OnlyIn(Dist.CLIENT)
public class AppContacts extends AbstractPhoneApp {

    private static final int STATE_LIST = 0;
    private static final int STATE_ADD  = 1;

    // Hauteur d'une ligne de contact
    private static final int ITEM_H = 26;

    private int state = STATE_LIST;
    private int scrollOffset = 0;
    private int maxVisible;

    // Widgets — liste
    private TextFieldWidget searchField;

    // Widgets — formulaire d'ajout
    private TextFieldWidget pseudoField;
    private TextFieldWidget numberField;
    private String errorMsg = "";

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    @Override
    protected void onInit() {
        // Nombre max de contacts visibles dans la liste
        // Espace dispo : phoneHeight - titleBar(16) - searchBar(16) - addBtn(20)
        maxVisible = (phoneHeight - 16 - 16 - 20) / ITEM_H;

        // Barre de recherche
        searchField = new TextFieldWidget(getFont(),
                phoneX + 4, phoneY + 20,
                phoneWidth - 8, 12,
                new StringTextComponent(""));
        searchField.setMaxLength(30);
        searchField.setBordered(false);
        searchField.setTextColor(0xFFFFFFFF);

        // Champs du formulaire d'ajout
        pseudoField = new TextFieldWidget(getFont(),
                phoneX + 8, phoneY + 46,
                phoneWidth - 16, 12,
                new StringTextComponent(""));
        pseudoField.setMaxLength(30);
        pseudoField.setTextColor(0xFFFFFFFF);

        numberField = new TextFieldWidget(getFont(),
                phoneX + 8, phoneY + 76,
                phoneWidth - 16, 12,
                new StringTextComponent(""));
        numberField.setMaxLength(14);
        numberField.setTextColor(0xFFFFFFFF);

        state = STATE_LIST;
        scrollOffset = 0;
        errorMsg = "";
    }

    // -------------------------------------------------------------------------
    // Tick (curseur clignotant des TextFieldWidget)
    // -------------------------------------------------------------------------

    @Override
    public void tick() {
        if (searchField != null) searchField.tick();
        if (pseudoField  != null) pseudoField.tick();
        if (numberField  != null) numberField.tick();
    }

    // -------------------------------------------------------------------------
    // Rendu
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (state == STATE_LIST) {
            renderList(stack, mouseX, mouseY, partialTicks);
        } else {
            renderAddForm(stack, mouseX, mouseY, partialTicks);
        }
    }

    // --- Vue liste ---

    private void renderList(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        drawTitleBar(stack, mouseX, mouseY);

        // Fond + barre de recherche
        PhoneRenderHelper.fillRect(stack, phoneX, phoneY + 16, phoneWidth, 16, 0xFF0D0D20);
        // Icone loupe
        PhoneFont.draw(stack, "?", phoneX + 4, phoneY + 19, 0xFF666688);
        searchField.x = phoneX + 14;
        searchField.y = phoneY + 20;
        searchField.setWidth(phoneWidth - 18);
        searchField.render(stack, mouseX, mouseY, partialTicks);

        // Separateur
        PhoneRenderHelper.fillRect(stack, phoneX, phoneY + 32, phoneWidth, 1, 0xFF222244);

        // Liste des contacts
        List<Contact> contacts = getFilteredContacts();
        int listTop = phoneY + 33;

        if (contacts.isEmpty()) {
            String msg = searchField.getValue().isEmpty() ? "Aucun contact" : "Aucun resultat";
            int mw = PhoneFont.width(msg);
            PhoneFont.draw(stack, msg,
                    phoneX + (phoneWidth - mw) / 2.0F,
                    phoneY + phoneHeight / 2.0F - 4,
                    0xFF555566);
        } else {
            for (int i = scrollOffset; i < contacts.size(); i++) {
                int rel = i - scrollOffset;
                if (rel >= maxVisible) break;

                Contact c = contacts.get(i);
                int itemY = listTop + rel * ITEM_H;

                // Fond de la ligne (zebre)
                int bg = (rel % 2 == 0) ? 0x22FFFFFF : 0x11FFFFFF;
                PhoneRenderHelper.fillRect(stack, phoneX, itemY, phoneWidth, ITEM_H - 1, bg);

                // Pseudo
                PhoneFont.draw(stack, c.getPseudo(), phoneX + 6, itemY + 3, 0xFFEEEEEE);

                // Numero (echelle 0.75)
                String num = c.getPhoneNumber().isEmpty() ? "Pas de numero" : c.getPhoneNumber();
                stack.pushPose();
                stack.translate(phoneX + 6, itemY + 14, 0);
                stack.scale(0.75F, 0.75F, 1.0F);
                PhoneFont.draw(stack, num, 0, 0, 0xFF777799);
                stack.popPose();

                // Boutons d'action — uniquement si le contact a un numero
                if (!c.getPhoneNumber().isEmpty()) {
                    int btnY = itemY + (ITEM_H - 12) / 2;

                    // Bouton SMS (bleu)
                    int smsX = phoneX + phoneWidth - 42;
                    boolean smsHov = isInBounds(mouseX, mouseY, smsX, btnY, 12, 12);
                    PhoneRenderHelper.fillRect(stack, smsX, btnY, 12, 12,
                            smsHov ? 0xFF3355CC : 0xFF112266);
                    PhoneFont.draw(stack, "\u2709", smsX + 2, btnY + 2, 0xFFFFFFFF);

                    // Bouton appeler (vert)
                    int callX = phoneX + phoneWidth - 28;
                    boolean callHov = isInBounds(mouseX, mouseY, callX, btnY, 12, 12);
                    PhoneRenderHelper.fillRect(stack, callX, btnY, 12, 12,
                            callHov ? 0xFF33CC55 : 0xFF116633);
                    PhoneFont.draw(stack, "\u260F", callX + 2, btnY + 2, 0xFFFFFFFF);
                }

                // Bouton supprimer "x"
                int delX = phoneX + phoneWidth - 14;
                int delY = itemY + (ITEM_H - 8) / 2;
                boolean delHov = isInBounds(mouseX, mouseY, delX, delY - 2, 10, 12);
                PhoneFont.draw(stack, "x", delX, delY,
                        delHov ? 0xFFFF4444 : 0xFF444466);
            }

            // Indicateurs de scroll
            if (scrollOffset > 0) {
                int arrY = listTop - 1;
                int arX = phoneX + phoneWidth / 2 - 2;
                PhoneFont.draw(stack, "^", arX, arrY, 0xFF555577);
            }
            if (scrollOffset + maxVisible < contacts.size()) {
                int arrY = listTop + maxVisible * ITEM_H;
                int arX = phoneX + phoneWidth / 2 - 2;
                PhoneFont.draw(stack, "v", arX, arrY, 0xFF555577);
            }
        }

        // Bouton "+ Ajouter" en bas
        int addBtnY = phoneY + phoneHeight - 18;
        boolean addHov = isInBounds(mouseX, mouseY, phoneX + phoneWidth / 2 - 22, addBtnY, 44, 14);
        PhoneRenderHelper.fillRect(stack,
                phoneX + phoneWidth / 2 - 22, addBtnY, 44, 14,
                addHov ? 0xFF3355CC : 0xFF1A2E88);
        String addLabel = "Ajouter";
        int alw = PhoneFont.width(addLabel);
        PhoneFont.draw(stack, addLabel,
                phoneX + (phoneWidth - alw) / 2.0F, addBtnY + 3, 0xFFFFFFFF);
    }

    // --- Formulaire d'ajout ---

    private void renderAddForm(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        drawTitleBar(stack, mouseX, mouseY);

        // Nom
        PhoneFont.draw(stack, "Nom *", phoneX + 8, phoneY + 34, 0xFFAAAAAA);
        PhoneRenderHelper.fillRect(stack, phoneX + 6, phoneY + 43, phoneWidth - 12, 14, 0xFF111128);
        pseudoField.y = phoneY + 46;
        pseudoField.render(stack, mouseX, mouseY, partialTicks);

        // Numero (optionnel)
        PhoneFont.draw(stack, "Numero (optionnel)", phoneX + 8, phoneY + 64, 0xFFAAAAAA);
        PhoneRenderHelper.fillRect(stack, phoneX + 6, phoneY + 73, phoneWidth - 12, 14, 0xFF111128);
        numberField.y = phoneY + 76;
        numberField.render(stack, mouseX, mouseY, partialTicks);

        // Message d'erreur
        if (!errorMsg.isEmpty()) {
            int ew = PhoneFont.width(errorMsg);
            PhoneFont.draw(stack, errorMsg,
                    phoneX + (phoneWidth - ew) / 2.0F,
                    phoneY + 93,
                    0xFFFF4444);
        }

        // Bouton Confirmer
        int confirmY = phoneY + 106;
        boolean confHov = isInBounds(mouseX, mouseY, phoneX + 10, confirmY, phoneWidth - 20, 14);
        PhoneRenderHelper.fillRect(stack, phoneX + 10, confirmY, phoneWidth - 20, 14,
                confHov ? 0xFF3355CC : 0xFF1A2E88);
        String confirmLabel = "Confirmer";
        int cw = PhoneFont.width(confirmLabel);
        PhoneFont.draw(stack, confirmLabel,
                phoneX + (phoneWidth - cw) / 2.0F, confirmY + 3, 0xFFFFFFFF);

        // Lien Annuler
        int cancelY = confirmY + 22;
        String cancelLabel = "Annuler";
        int canw = PhoneFont.width(cancelLabel);
        boolean canHov = isInBounds(mouseX, mouseY,
                phoneX + (phoneWidth - canw) / 2, cancelY, canw, 9);
        PhoneFont.draw(stack, cancelLabel,
                phoneX + (phoneWidth - canw) / 2.0F, cancelY,
                canHov ? 0xFFFFFFFF : 0xFF888899);
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleBackButtonClick(mouseX, mouseY)) return true;

        if (state == STATE_LIST) {
            return handleListClick(mouseX, mouseY, button);
        } else {
            return handleAddFormClick(mouseX, mouseY, button);
        }
    }

    private boolean handleListClick(double mx, double my, int button) {
        // Champ de recherche
        if (searchField.mouseClicked(mx, my, button)) {
            return true;
        }

        // Bouton "+ Ajouter"
        int addBtnY = phoneY + phoneHeight - 18;
        if (isInBounds(mx, my, phoneX + phoneWidth / 2 - 22, addBtnY, 44, 14)) {
            openAddForm();
            return true;
        }

        // Boutons sur les lignes
        List<Contact> contacts = getFilteredContacts();
        int listTop = phoneY + 33;
        for (int i = scrollOffset; i < contacts.size(); i++) {
            int rel = i - scrollOffset;
            if (rel >= maxVisible) break;
            Contact c = contacts.get(i);
            int itemY = listTop + rel * ITEM_H;

            // Boutons d'action
            if (!c.getPhoneNumber().isEmpty()) {
                int btnY = itemY + (ITEM_H - 12) / 2;

                // Bouton SMS
                int smsX = phoneX + phoneWidth - 42;
                if (isInBounds(mx, my, smsX, btnY, 12, 12)) {
                    messageContact(c);
                    return true;
                }

                // Bouton appeler
                int callX = phoneX + phoneWidth - 28;
                if (isInBounds(mx, my, callX, btnY, 12, 12)) {
                    callContact(c);
                    return true;
                }
            }

            // Bouton supprimer
            int delX = phoneX + phoneWidth - 14;
            int delY = itemY + (ITEM_H - 8) / 2;
            if (isInBounds(mx, my, delX, delY - 2, 10, 12)) {
                PacketHandler.CHANNEL.sendToServer(
                        new PacketRemoveContact(phoneScreen.getPhoneNumber(),
                                c.getUuid().toString()));
                return true;
            }
        }
        return false;
    }

    private boolean handleAddFormClick(double mx, double my, int button) {
        // Focus des champs
        pseudoField.mouseClicked(mx, my, button);
        numberField.mouseClicked(mx, my, button);

        // Bouton Confirmer
        int confirmY = phoneY + 106;
        if (isInBounds(mx, my, phoneX + 10, confirmY, phoneWidth - 20, 14)) {
            submitAddContact();
            return true;
        }

        // Annuler
        int cancelY = confirmY + 22;
        String cancelLabel = "Annuler";
        int canw = PhoneFont.width(cancelLabel);
        if (isInBounds(mx, my, phoneX + (phoneWidth - canw) / 2, cancelY, canw, 9)) {
            state = STATE_LIST;
            errorMsg = "";
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (state == STATE_LIST) {
            List<Contact> contacts = getFilteredContacts();
            int maxScroll = Math.max(0, contacts.size() - maxVisible);
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (state == STATE_ADD) {
            if (pseudoField.isFocused() && pseudoField.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (numberField.isFocused()) {
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    // Supprime le dernier chiffre et reformate
                    String raw = numberField.getValue().replaceAll("[^0-9]", "");
                    if (!raw.isEmpty()) {
                        numberField.setValue(formatPhoneDigits(raw.substring(0, raw.length() - 1)));
                    }
                    return true;
                }
                if (numberField.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                submitAddContact();
                return true;
            }
            // Tab pour passer d'un champ a l'autre
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (pseudoField.isFocused()) {
                    pseudoField.changeFocus(false); // unfocus pseudo
                    numberField.changeFocus(true);  // focus number
                } else {
                    numberField.changeFocus(false);
                    pseudoField.changeFocus(true);
                }
                return true;
            }
        } else {
            if (searchField.isFocused() && searchField.keyPressed(keyCode, scanCode, modifiers)) {
                scrollOffset = 0; // reset scroll a chaque frappe
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (state == STATE_ADD) {
            if (pseudoField.isFocused()) return pseudoField.charTyped(codePoint, modifiers);
            if (numberField.isFocused()) {
                // Chiffres uniquement, max 10 chiffres
                if (Character.isDigit(codePoint)) {
                    String raw = numberField.getValue().replaceAll("[^0-9]", "");
                    if (raw.length() < 10) {
                        numberField.setValue(formatPhoneDigits(raw + codePoint));
                    }
                }
                return true; // bloque tout caractere non-chiffre
            }
        } else {
            if (searchField.isFocused()) {
                boolean result = searchField.charTyped(codePoint, modifiers);
                scrollOffset = 0;
                return result;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Demarre un appel vers un contact et navigue vers AppPhone. */
    private void callContact(Contact c) {
        AppPhone appPhone = new AppPhone();
        phoneScreen.navigateTo(appPhone);
        appPhone.startCall(c.getPhoneNumber(), c.getPseudo());
    }

    /** Ouvre la conversation SMS avec un contact et navigue vers AppSMS. */
    private void messageContact(Contact c) {
        AppSMS appSMS = new AppSMS();
        phoneScreen.navigateTo(appSMS);
        appSMS.openConversationWith(c.getPhoneNumber());
    }

    private void openAddForm() {
        state = STATE_ADD;
        errorMsg = "";
        pseudoField.setValue("");
        numberField.setValue("");
        // Auto-focus le champ pseudo a l'ouverture
        if (!pseudoField.isFocused()) pseudoField.changeFocus(true);
    }

    private void submitAddContact() {
        String pseudo = pseudoField.getValue().trim();
        if (pseudo.isEmpty()) {
            errorMsg = "Le nom est obligatoire";
            return;
        }
        String number = numberField.getValue().trim();
        PacketHandler.CHANNEL.sendToServer(
                new PacketAddContact(phoneScreen.getPhoneNumber(), pseudo, number));
        state = STATE_LIST;
        errorMsg = "";
    }

    private List<Contact> getFilteredContacts() {
        List<Contact> all = PhoneData.getContacts(getPhoneStack());
        String query = (searchField != null) ? searchField.getValue().trim().toLowerCase() : "";
        if (query.isEmpty()) return all;
        return all.stream()
                .filter(c -> c.getPseudo().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    /**
     * Formate une chaine de chiffres en groupes de 2 separes par des espaces.
     * Ex: "0612345678" -> "06 12 34 56 78"
     */
    private static String formatPhoneDigits(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 2 == 0) sb.append(' ');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    private boolean isInBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public String getTitle() {
        return state == STATE_ADD ? "Nouveau contact" : "Contacts";
    }

    @Override
    public void onBack() {
        if (state == STATE_ADD) {
            // Retour a la liste, pas a l'accueil
            state = STATE_LIST;
            errorMsg = "";
        } else {
            super.onBack();
        }
    }
}
