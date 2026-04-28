package com.districtlife.phone.screen.hud;

import com.districtlife.phone.call.PhoneCallState;
import com.districtlife.phone.capability.Contact;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import com.districtlife.phone.screen.PhoneFixScreen;
import com.districtlife.phone.screen.PhoneScreen;
import com.districtlife.phone.util.PhoneRenderHelper;
import net.minecraft.item.ItemStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.districtlife.phone.util.PhoneFont;

/**
 * Barre de notification en haut de l'ecran quand un appel est en cours
 * et que le PhoneScreen n'est pas ouvert.
 */
@OnlyIn(Dist.CLIENT)
public class PhoneCallHud {

    private static final ResourceLocation TEX_HUD_BG =
            new ResourceLocation("districtlife_phone", "textures/gui/bg_call_hud.png");

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen instanceof PhoneScreen) return;   // l'app gere l'affichage
        if (mc.screen instanceof PhoneFixScreen) return; // le boitier gere l'affichage

        PhoneCallState.CallState state = PhoneCallState.getState();
        if (state == PhoneCallState.CallState.IDLE) return;

        MatrixStack stack  = event.getMatrixStack();
        int screenW = mc.getWindow().getGuiScaledWidth();

        String line1;
        String line2 = "";
        int bgColor;
        int textColor;

        switch (state) {
            case RINGING:
                line1     = "Appel entrant de " + resolveCallerDisplay(mc, PhoneCallState.getOtherPhone());
                line2     = "Ouvrez votre telephone pour repondre";
                bgColor   = 0xCC1A3366;
                textColor = 0xFFFFFFFF;
                break;
            case CALLING:
                line1     = "Appel en cours vers " + resolveCallerDisplay(mc, PhoneCallState.getOtherPhone());
                bgColor   = 0xCC1A3366;
                textColor = 0xFFCCCCFF;
                break;
            case INCALL:
                long now     = mc.player.level.getGameTime();
                long elapsed = Math.max(0, now - PhoneCallState.getCallStartTick());
                long sec     = (elapsed / 20) % 60;
                long min     = (elapsed / 20) / 60;
                line1     = "En communication  " + String.format("%02d:%02d", min, sec);
                bgColor   = 0xCC0D3322;
                textColor = 0xFF88FF88;
                break;
            default:
                return;
        }

        int barH = line2.isEmpty() ? 14 : 24;
        PhoneRenderHelper.drawTextureScaled(stack, TEX_HUD_BG, 0, 0, screenW, barH, 256, 24);

        PhoneFont.draw(stack, line1, (screenW - PhoneFont.width(line1)) / 2.0F, 3, textColor);
        if (!line2.isEmpty()) {
            PhoneFont.draw(stack, line2, (screenW - PhoneFont.width(line2)) / 2.0F, 14, 0xFFAAAACC);
        }
    }

    /** Retourne le nom du contact si present dans les contacts, sinon le numero. */
    private static String resolveCallerDisplay(Minecraft mc, String callerPhone) {
        if (mc.player == null) return callerPhone;
        ItemStack phone = PhoneItem.findFirstPhoneStack(mc.player);
        if (phone.isEmpty()) return callerPhone;
        for (Contact c : PhoneData.getContacts(phone)) {
            if (c.getPhoneNumber().equals(callerPhone)) return c.getPseudo();
        }
        return callerPhone;
    }

}
