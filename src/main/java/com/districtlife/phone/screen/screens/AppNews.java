package com.districtlife.phone.screen.screens;

import com.districtlife.phone.news.NewsArticle;
import com.districtlife.phone.news.NewsClientCache;
import com.districtlife.phone.screen.AbstractPhoneApp;
import com.districtlife.phone.util.PhoneRenderHelper;
import com.districtlife.phone.util.RPTime;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * App News — journal RP publie par les modos via /news publish.
 *
 * Deux vues :
 *   LIST    : liste scrollable des articles (titre + auteur + date + apercu)
 *   ARTICLE : lecture complete avec scroll vertical
 */
@OnlyIn(Dist.CLIENT)
public class AppNews extends AbstractPhoneApp {

    private enum View { LIST, ARTICLE }

    private static final int BAR_H    = 16;
    private static final int ROW_H    = 44;  // hauteur d'une ligne de liste
    private static final int LINE_H   = 10;  // espacement entre lignes de contenu

    private View         currentView     = View.LIST;
    private NewsArticle  selectedArticle = null;

    private int listScroll    = 0;
    private int articleScroll = 0;

    /** Lignes de contenu calculees (word-wrap) pour l'article courant. */
    private final List<String> wrappedLines = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void onInit() {
        currentView     = View.LIST;
        selectedArticle = null;
        listScroll      = 0;
        articleScroll   = 0;
        wrappedLines.clear();

        // Marquer comme lu a l'ouverture de l'app
        NewsClientCache.markAllRead();
    }

    // -------------------------------------------------------------------------
    // Rendu principal
    // -------------------------------------------------------------------------

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partial) {
        drawTitleBar(stack, mouseX, mouseY);

        int contentY = phoneY + BAR_H;
        int contentH = phoneHeight - BAR_H;

        if (currentView == View.LIST) {
            renderList(stack, mouseX, mouseY, contentY, contentH);
        } else {
            renderArticle(stack, mouseX, mouseY, contentY, contentH);
        }
    }

    // -------------------------------------------------------------------------
    // Vue : liste
    // -------------------------------------------------------------------------

    private void renderList(MatrixStack stack, int mouseX, int mouseY,
                             int contentY, int contentH) {
        List<NewsArticle> all = NewsClientCache.getAll();

        if (all.isEmpty()) {
            String msg = "Aucun article disponible";
            getFont().draw(stack, msg,
                    phoneX + (phoneWidth - getFont().width(msg)) / 2.0F,
                    contentY + contentH / 2.0F - 4,
                    0xFF666677);
            return;
        }

        int maxVis = contentH / ROW_H;

        for (int i = listScroll; i < all.size(); i++) {
            int rel  = i - listScroll;
            if (rel >= maxVis) break;

            NewsArticle a = all.get(i);
            int rowY = contentY + rel * ROW_H;

            boolean hov = isIn(mouseX, mouseY, phoneX, rowY, phoneWidth, ROW_H - 1);
            PhoneRenderHelper.fillRect(stack, phoneX, rowY, phoneWidth, ROW_H - 1,
                    hov ? 0x33FFFFFF : (rel % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF));

            // Titre
            String title = truncate(a.title, phoneWidth - 10);
            getFont().draw(stack, title, phoneX + 6, rowY + 4, 0xFFEEEEEE);

            // Auteur + date (echelle 0.75)
            String dateLine = a.author + "  \u2022  " + formatDate(a.tickRP);
            renderSmall(stack, dateLine, phoneX + 6, rowY + 15, 0xFF7788AA);

            // Apercu du contenu (1 ligne, echelle 0.75)
            String preview = truncate(a.content.replace('\n', ' '), phoneWidth - 10);
            renderSmall(stack, preview, phoneX + 6, rowY + 25, 0xFF556677);

            // Separateur
            PhoneRenderHelper.fillRect(stack, phoneX + 4, rowY + ROW_H - 1,
                    phoneWidth - 8, 1, 0xFF2A2A3E);
        }

        // Indicateurs de scroll
        if (listScroll > 0) {
            getFont().draw(stack, "\u2303", phoneX + phoneWidth / 2 - 3, contentY + 1, 0xFF555577);
        }
        if (listScroll + maxVis < all.size()) {
            getFont().draw(stack, "\u2304", phoneX + phoneWidth / 2 - 3,
                    contentY + maxVis * ROW_H + 1, 0xFF555577);
        }
    }

    // -------------------------------------------------------------------------
    // Vue : article complet
    // -------------------------------------------------------------------------

    private void renderArticle(MatrixStack stack, int mouseX, int mouseY,
                                int contentY, int contentH) {
        if (selectedArticle == null) return;

        // --- En-tete ---
        int headerH = 38;

        // Titre de l'article (avec retour a la ligne si trop long)
        String title = selectedArticle.title;
        getFont().draw(stack, truncate(title, phoneWidth - 10),
                phoneX + 5, contentY + 4, 0xFFEEEEFF);

        // Auteur + date
        String meta = selectedArticle.author + "  \u2022  " + formatDate(selectedArticle.tickRP);
        renderSmall(stack, meta, phoneX + 5, contentY + 16, 0xFF7788AA);

        // Separateur
        PhoneRenderHelper.fillRect(stack, phoneX + 4, contentY + headerH - 3,
                phoneWidth - 8, 1, 0xFF334466);

        // --- Contenu scrollable ---
        int textAreaY = contentY + headerH;
        int textAreaH = contentH - headerH;
        int textW     = phoneWidth - 16;   // 8px marge de chaque cote
        int maxVis    = textAreaH / LINE_H;

        // Clip visuel (fond noir pour eviter le debordement sous l'en-tete)
        PhoneRenderHelper.fillRect(stack, phoneX, textAreaY, phoneWidth, textAreaH, 0xFF0D1117);

        for (int i = articleScroll; i < wrappedLines.size(); i++) {
            int rel = i - articleScroll;
            if (rel >= maxVis) break;

            String line = wrappedLines.get(i);
            int lineY = textAreaY + rel * LINE_H + 3;
            getFont().draw(stack, line, phoneX + 8, lineY, 0xFFCCCCDD);
        }

        // Indicateur de scroll bas
        if (articleScroll + maxVis < wrappedLines.size()) {
            getFont().draw(stack, "\u2304",
                    phoneX + phoneWidth / 2 - 3,
                    textAreaY + textAreaH - 10, 0xFF555577);
        }
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleBackButtonClick(mx, my)) return true;

        if (currentView == View.LIST) {
            return handleListClick(mx, my);
        }
        return false;
    }

    private boolean handleListClick(double mx, double my) {
        List<NewsArticle> all = NewsClientCache.getAll();
        int contentY = phoneY + BAR_H;
        int maxVis   = (phoneHeight - BAR_H) / ROW_H;

        for (int i = listScroll; i < all.size(); i++) {
            int rel  = i - listScroll;
            if (rel >= maxVis) break;
            int rowY = contentY + rel * ROW_H;
            if (isIn(mx, my, phoneX, rowY, phoneWidth, ROW_H)) {
                openArticle(all.get(i));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (currentView == View.LIST) {
            List<NewsArticle> all = NewsClientCache.getAll();
            int maxVis = (phoneHeight - BAR_H) / ROW_H;
            int max    = Math.max(0, all.size() - maxVis);
            listScroll = (int) Math.max(0, Math.min(max, listScroll - delta));
        } else {
            int textAreaH = phoneHeight - BAR_H - 38;
            int maxVis    = textAreaH / LINE_H;
            int max       = Math.max(0, wrappedLines.size() - maxVis);
            articleScroll = (int) Math.max(0, Math.min(max, articleScroll - delta * 3));
        }
        return true;
    }

    @Override
    public void onBack() {
        if (currentView == View.ARTICLE) {
            currentView     = View.LIST;
            selectedArticle = null;
            wrappedLines.clear();
        } else {
            super.onBack();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void openArticle(NewsArticle article) {
        selectedArticle = article;
        currentView     = View.ARTICLE;
        articleScroll   = 0;

        // Pre-calcule le word-wrap pour le contenu
        wrappedLines.clear();
        int textW = phoneWidth - 16;
        for (String line : wrapText(article.content, textW)) {
            wrappedLines.add(line);
        }
    }

    /**
     * Decoupe le texte en lignes d'au plus maxWidth pixels (en respectant les espaces).
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }
            String remaining = paragraph;
            while (!remaining.isEmpty()) {
                if (getFont().width(remaining) <= maxWidth) {
                    result.add(remaining);
                    break;
                }
                // Trouve la coupure maximale qui rentre dans maxWidth
                int cut = 0;
                int lastSpace = -1;
                for (int i = 1; i <= remaining.length(); i++) {
                    if (getFont().width(remaining.substring(0, i)) > maxWidth) break;
                    cut = i;
                    if (remaining.charAt(i - 1) == ' ') lastSpace = i - 1;
                }
                int breakAt = lastSpace > 0 ? lastSpace : Math.max(1, cut);
                result.add(remaining.substring(0, breakAt));
                remaining = remaining.substring(breakAt).trim();
            }
        }
        return result;
    }

    /** Formate le tick RP en date lisible ("Lundi 14 Avril - An 1"). */
    private static String formatDate(long tickRP) {
        return new RPTime(tickRP).getFormattedDate();
    }

    /** Tronque un texte si son rendu depasse maxWidth pixels, ajoute "...". */
    private String truncate(String text, int maxWidth) {
        if (getFont().width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisW = getFont().width(ellipsis);
        int i = text.length();
        while (i > 0 && getFont().width(text.substring(0, i)) + ellipsisW > maxWidth) i--;
        return text.substring(0, i) + ellipsis;
    }

    private void renderSmall(MatrixStack stack, String text, int x, int y, int color) {
        stack.pushPose();
        stack.translate(x, y, 0);
        stack.scale(0.75F, 0.75F, 1F);
        getFont().draw(stack, text, 0, 0, color);
        stack.popPose();
    }

    private boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // -------------------------------------------------------------------------
    // Titre
    // -------------------------------------------------------------------------

    @Override
    public String getTitle() {
        if (currentView == View.ARTICLE && selectedArticle != null) {
            return truncate(selectedArticle.title, phoneWidth - 20);
        }
        return "Actualites";
    }
}
