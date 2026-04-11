package com.districtlife.phone.network;

import com.districtlife.phone.call.PhoneCallState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoye SERVER -> CLIENT : mise a jour de l'etat d'un appel. */
public class PacketCallUpdate {

    public enum Signal {
        ACCEPTED(0),
        DECLINED(1),
        ENDED(2),
        BUSY(3),
        UNAVAILABLE(4);

        private final int id;

        Signal(int id) { this.id = id; }

        public int getId() { return id; }

        public static Signal fromId(int id) {
            for (Signal s : values()) if (s.id == id) return s;
            return ENDED;
        }
    }

    private final Signal signal;
    private final String otherPhone;
    private final long   callStartTick; // valide uniquement pour ACCEPTED

    public PacketCallUpdate(Signal signal, String otherPhone, long callStartTick) {
        this.signal        = signal;
        this.otherPhone    = otherPhone;
        this.callStartTick = callStartTick;
    }

    public static void encode(PacketCallUpdate p, PacketBuffer buf) {
        buf.writeByte(p.signal.getId());
        buf.writeUtf(p.otherPhone, 20);
        buf.writeLong(p.callStartTick);
    }

    public static PacketCallUpdate decode(PacketBuffer buf) {
        return new PacketCallUpdate(Signal.fromId(buf.readByte()), buf.readUtf(20), buf.readLong());
    }

    public static void handle(PacketCallUpdate packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;

            switch (packet.signal) {
                case ACCEPTED:
                    PhoneCallState.setInCall(packet.callStartTick);
                    break;
                case DECLINED:
                case ENDED:
                case BUSY:
                case UNAVAILABLE:
                    PhoneCallState.reset();
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
