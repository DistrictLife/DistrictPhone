package com.districtlife.phone.network;

import com.districtlife.phone.PhoneMod;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PhoneMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        // SERVER -> CLIENT
        CHANNEL.registerMessage(id++, PacketSyncPhone.class,
                PacketSyncPhone::encode, PacketSyncPhone::decode, PacketSyncPhone::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, PacketCallRequest.class,
                PacketCallRequest::encode, PacketCallRequest::decode, PacketCallRequest::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, PacketCallUpdate.class,
                PacketCallUpdate::encode, PacketCallUpdate::decode, PacketCallUpdate::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // CLIENT -> SERVER
        CHANNEL.registerMessage(id++, PacketAddContact.class,
                PacketAddContact::encode, PacketAddContact::decode, PacketAddContact::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, PacketRemoveContact.class,
                PacketRemoveContact::encode, PacketRemoveContact::decode, PacketRemoveContact::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, PacketSendSMS.class,
                PacketSendSMS::encode, PacketSendSMS::decode, PacketSendSMS::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, PacketCallSignal.class,
                PacketCallSignal::encode, PacketCallSignal::decode, PacketCallSignal::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // SERVER -> CLIENT : news
        CHANNEL.registerMessage(id++, PacketReceiveNews.class,
                PacketReceiveNews::encode, PacketReceiveNews::decode, PacketReceiveNews::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, PacketSyncNews.class,
                PacketSyncNews::encode, PacketSyncNews::decode, PacketSyncNews::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // CLIENT -> SERVER : photo
        CHANNEL.registerMessage(id++, PacketAddPhoto.class,
                PacketAddPhoto::encode, PacketAddPhoto::decode, PacketAddPhoto::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // CLIENT -> SERVER : masquage d'application
        CHANNEL.registerMessage(id++, PacketSetAppHidden.class,
                PacketSetAppHidden::encode, PacketSetAppHidden::decode, PacketSetAppHidden::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // SERVER -> CLIENT : configuration Dynmap
        CHANNEL.registerMessage(id++, PacketSyncDynmap.class,
                PacketSyncDynmap::encode, PacketSyncDynmap::decode, PacketSyncDynmap::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // SERVER -> CLIENT : points de carte
        CHANNEL.registerMessage(id++, PacketSyncMapPoints.class,
                PacketSyncMapPoints::encode, PacketSyncMapPoints::decode, PacketSyncMapPoints::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // SERVER -> CLIENT : debug texture viewer (OP uniquement)
        CHANNEL.registerMessage(id++, PacketOpenDebugTexture.class,
                PacketOpenDebugTexture::encode, PacketOpenDebugTexture::decode, PacketOpenDebugTexture::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // SERVER -> CLIENT : notification SMS entrant
        CHANNEL.registerMessage(id++, PacketSmsNotify.class,
                PacketSmsNotify::encode, PacketSmsNotify::decode, PacketSmsNotify::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToPlayer(Object packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
