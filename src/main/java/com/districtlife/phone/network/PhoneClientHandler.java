package com.districtlife.phone.network;

import com.districtlife.phone.call.PhoneCallState;
import com.districtlife.phone.call.PhoneCallState.CallState;
import com.districtlife.phone.camera.CameraHelper;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import com.districtlife.phone.news.NewsClientCache;
import com.districtlife.phone.registry.ModSounds;
import com.districtlife.phone.screen.screens.DebugTextureScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Gestionnaire client-only des evenements reseau.
 *
 * Enregistre via : MinecraftForge.EVENT_BUS.register(PhoneClientHandler.class)
 * Cette instruction utilise un LDC (class literal), qui est a resolution paresseuse.
 * La classe n'est donc jamais chargee sur serveur dedie.
 *
 * @OnlyIn(CLIENT) au niveau classe = filet de securite supplementaire.
 */
@OnlyIn(Dist.CLIENT)
public class PhoneClientHandler {

    // Repete la sonnerie toutes les 80 ticks (4 s) tant que l'etat ne change pas
    private static int ringTick = 0;

    @SubscribeEvent
    public static void onSyncPhone(PhoneNetEvent.SyncPhone event) {
        if (Minecraft.getInstance().player == null) return;
        ItemStack stack = PhoneItem.findPhoneStack(
                Minecraft.getInstance().player, event.phoneNumber);
        if (!stack.isEmpty()) {
            PhoneData.setRaw(stack, event.data);
        }
    }

    @SubscribeEvent
    public static void onCallRequest(PhoneNetEvent.CallRequest event) {
        if (Minecraft.getInstance().player == null) return;
        PhoneCallState.setRinging(event.callerPhone, event.callerMcName);
    }

    @SubscribeEvent
    public static void onReceiveNews(PhoneNetEvent.ReceiveNews event) {
        NewsClientCache.addOrUpdate(event.article);
    }

    @SubscribeEvent
    public static void onSyncNews(PhoneNetEvent.SyncNews event) {
        NewsClientCache.setAll(event.articles);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        CameraHelper.onClientTick();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        CallState state = PhoneCallState.getState();
        if (state == CallState.RINGING) {
            if (ringTick % 80 == 0)
                mc.player.playSound(ModSounds.PHONE_RING.get(), 1.0f, 1.0f);
            ringTick++;
        } else if (state == CallState.CALLING) {
            if (ringTick % 80 == 0)
                mc.player.playSound(ModSounds.PHONE_RINGBACK.get(), 0.7f, 1.0f);
            ringTick++;
        } else {
            ringTick = 0;
        }
    }

    @SubscribeEvent
    public static void onSmsNotify(PhoneNetEvent.SmsNotify event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.playSound(ModSounds.FX_SMS_RECEIVE.get(), 0.6f, 1.0f);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        CameraHelper.onRenderTick();
    }

    @SubscribeEvent
    public static void onOpenDebugTexture(PhoneNetEvent.OpenDebugTexture event) {
        Minecraft.getInstance().setScreen(new DebugTextureScreen(event.texturePath));
    }

    @SubscribeEvent
    public static void onCallUpdate(PhoneNetEvent.CallUpdate event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        switch (event.signal) {
            case ACCEPTED:
                PhoneCallState.setInCall(event.callStartTick);
                break;
            case DECLINED:
            case ENDED:
                PhoneCallState.reset();
                mc.player.playSound(ModSounds.PHONE_HANGUP.get(), 0.8f, 1.0f);
                break;
            case BUSY:
            case UNAVAILABLE:
                PhoneCallState.setImpossible();
                mc.player.playSound(ModSounds.PHONE_HANGUP.get(), 0.8f, 1.0f);
                break;
        }
    }
}
