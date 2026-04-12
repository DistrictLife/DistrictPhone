package com.districtlife.phone.network;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.eventbus.api.Event;

/**
 * Evenements reseau tires par les paquets SERVER->CLIENT.
 * Classes plain (aucun @OnlyIn) : peuvent etre referencees depuis n'importe quelle classe.
 * Le traitement Minecraft est dans PhoneClientHandler, jamais charge sur serveur.
 */
public final class PhoneNetEvent {

    private PhoneNetEvent() {}

    public static class SyncPhone extends Event {
        public final String     phoneNumber;
        public final CompoundNBT data;
        public SyncPhone(String phoneNumber, CompoundNBT data) {
            this.phoneNumber = phoneNumber;
            this.data        = data;
        }
    }

    public static class CallRequest extends Event {
        public final String callerPhone;
        public final String callerMcName;
        public CallRequest(String callerPhone, String callerMcName) {
            this.callerPhone  = callerPhone;
            this.callerMcName = callerMcName;
        }
    }

    public static class CallUpdate extends Event {
        public final PacketCallUpdate.Signal signal;
        public final long                    callStartTick;
        public CallUpdate(PacketCallUpdate.Signal signal, long callStartTick) {
            this.signal        = signal;
            this.callStartTick = callStartTick;
        }
    }
}
