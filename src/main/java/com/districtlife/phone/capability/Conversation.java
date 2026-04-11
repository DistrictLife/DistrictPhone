package com.districtlife.phone.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Represente une conversation SMS avec un autre joueur.
 * Identifiee par le numero de telephone de l'autre partie.
 */
public class Conversation {

    /** Numero de l'autre partie - cle primaire. */
    private String contactPhoneNumber;
    private List<Message> messages;

    public Conversation(String contactPhoneNumber) {
        this.contactPhoneNumber = contactPhoneNumber;
        this.messages = new ArrayList<>();
    }

    public String getContactPhoneNumber() { return contactPhoneNumber; }
    public List<Message> getMessages()    { return messages; }

    public void addMessage(Message message) {
        messages.add(message);
    }

    /** Retourne le dernier message, ou null si la conversation est vide. */
    public Message getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    public CompoundNBT writeToNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString("phoneNumber", contactPhoneNumber);
        ListNBT list = new ListNBT();
        for (Message msg : messages) list.add(msg.writeToNBT());
        nbt.put("messages", list);
        return nbt;
    }

    public static Conversation readFromNBT(CompoundNBT nbt) {
        String phone = nbt.getString("phoneNumber");
        Conversation conv = new Conversation(phone);
        ListNBT list = nbt.getList("messages", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            conv.messages.add(Message.readFromNBT(list.getCompound(i)));
        }
        return conv;
    }

    // -------------------------------------------------------------------------
    // Message
    // -------------------------------------------------------------------------

    public static class Message {
        private final String text;
        private final boolean outgoing; // true = envoye par ce joueur
        private final long timestamp;   // world game time

        public Message(String text, boolean outgoing, long timestamp) {
            this.text      = text;
            this.outgoing  = outgoing;
            this.timestamp = timestamp;
        }

        public String  getText()      { return text; }
        public boolean isOutgoing()   { return outgoing; }
        public long    getTimestamp() { return timestamp; }

        public CompoundNBT writeToNBT() {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putString("text",      text);
            nbt.putBoolean("outgoing", outgoing);
            nbt.putLong("timestamp",   timestamp);
            return nbt;
        }

        public static Message readFromNBT(CompoundNBT nbt) {
            return new Message(
                    nbt.getString("text"),
                    nbt.getBoolean("outgoing"),
                    nbt.getLong("timestamp"));
        }
    }
}
