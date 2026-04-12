package com.districtlife.phone.network;

import com.districtlife.phone.call.PhoneCallState;
import com.districtlife.phone.camera.CameraHelper;
import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import com.districtlife.phone.news.NewsClientCache;
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
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        CameraHelper.onRenderTick();
    }

    @SubscribeEvent
    public static void onCallUpdate(PhoneNetEvent.CallUpdate event) {
        if (Minecraft.getInstance().player == null) return;
        switch (event.signal) {
            case ACCEPTED:
                PhoneCallState.setInCall(event.callStartTick);
                break;
            case DECLINED:
            case ENDED:
            case BUSY:
                PhoneCallState.reset();
                break;
            case UNAVAILABLE:
                PhoneCallState.setImpossible();
                break;
        }
    }
}
