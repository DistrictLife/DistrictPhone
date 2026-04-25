package com.districtlife.phone.registry;

import com.districtlife.phone.PhoneMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PhoneMod.MOD_ID);

    /** Sonnerie du telephone mobile (appel entrant). */
    public static final RegistryObject<SoundEvent> PHONE_RING =
            reg("phone.ring");

    /** Sonnerie du telephone fixe (appel entrant). */
    public static final RegistryObject<SoundEvent> PHONE_RING_FIX =
            reg("phone.ring_fix");

    /** Tonalite de retour d'appel (entendue par l'appelant en attendant le decroché). */
    public static final RegistryObject<SoundEvent> PHONE_RINGBACK =
            reg("phone.ringback");

    /** Son de fin d'appel / raccroché. */
    public static final RegistryObject<SoundEvent> PHONE_HANGUP =
            reg("phone.hangup");

    /** Son d'envoi d'un SMS. */
    public static final RegistryObject<SoundEvent> FX_SMS_SEND =
            reg("fx.sms_send");

    /** Son de reception d'un SMS. */
    public static final RegistryObject<SoundEvent> FX_SMS_RECEIVE =
            reg("fx.sms_receive");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name,
                () -> new SoundEvent(new ResourceLocation(PhoneMod.MOD_ID, name)));
    }
}
