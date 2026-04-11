package com.districtlife.phone.data;

import com.districtlife.phone.capability.Contact;
import com.districtlife.phone.capability.Conversation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Acces statique aux donnees stockees dans le NBT du telephone (ItemStack).
 * Toutes les donnees sont sous la cle "PhoneData" dans le tag de l'item.
 */
public final class PhoneData {

    public static final String NBT_KEY = "PhoneData";

    private PhoneData() {}

    // -------------------------------------------------------------------------
    // Acces interne
    // -------------------------------------------------------------------------

    /** Retourne (en creant si necessaire) le sous-compound "PhoneData" de l'item. */
    private static CompoundNBT get(ItemStack stack) {
        CompoundNBT root = stack.getOrCreateTag();
        if (!root.contains(NBT_KEY, Constants.NBT.TAG_COMPOUND)) {
            root.put(NBT_KEY, new CompoundNBT());
        }
        return root.getCompound(NBT_KEY);
    }

    /** Retourne une copie du sous-compound (pour la serialisation reseau). */
    public static CompoundNBT getRaw(ItemStack stack) {
        if (stack.isEmpty()) return new CompoundNBT();
        CompoundNBT root = stack.getTag();
        if (root == null || !root.contains(NBT_KEY)) return new CompoundNBT();
        return root.getCompound(NBT_KEY).copy();
    }

    /** Ecrase le sous-compound avec les donnees recues (depuis PacketSyncPhone). */
    public static void setRaw(ItemStack stack, CompoundNBT data) {
        if (stack.isEmpty()) return;
        stack.getOrCreateTag().put(NBT_KEY, data);
    }

    // -------------------------------------------------------------------------
    // Contacts
    // -------------------------------------------------------------------------

    public static List<Contact> getContacts(ItemStack stack) {
        if (stack.isEmpty()) return new ArrayList<>();
        List<Contact> contacts = new ArrayList<>();
        ListNBT list = get(stack).getList("contacts", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            contacts.add(Contact.readFromNBT(list.getCompound(i)));
        }
        return contacts;
    }

    public static void addContact(ItemStack stack, Contact contact) {
        if (stack.isEmpty()) return;
        CompoundNBT data = get(stack);
        ListNBT list = data.getList("contacts", Constants.NBT.TAG_COMPOUND);
        list.add(contact.writeToNBT());
        data.put("contacts", list);
    }

    public static void removeContact(ItemStack stack, String uuid) {
        if (stack.isEmpty()) return;
        CompoundNBT data = get(stack);
        ListNBT old = data.getList("contacts", Constants.NBT.TAG_COMPOUND);
        ListNBT updated = new ListNBT();
        for (int i = 0; i < old.size(); i++) {
            CompoundNBT entry = old.getCompound(i);
            try {
                if (!entry.getUUID("uuid").toString().equals(uuid)) {
                    updated.add(entry);
                }
            } catch (Exception e) {
                updated.add(entry);
            }
        }
        data.put("contacts", updated);
    }

    // -------------------------------------------------------------------------
    // Conversations / SMS
    // -------------------------------------------------------------------------

    public static List<Conversation> getConversations(ItemStack stack) {
        if (stack.isEmpty()) return new ArrayList<>();
        List<Conversation> convs = new ArrayList<>();
        ListNBT list = get(stack).getList("sms", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            convs.add(Conversation.readFromNBT(list.getCompound(i)));
        }
        return convs;
    }

    public static Conversation findConversation(ItemStack stack, String phoneNumber) {
        if (stack.isEmpty()) return null;
        ListNBT list = get(stack).getList("sms", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT entry = list.getCompound(i);
            if (entry.getString("phoneNumber").equals(phoneNumber)) {
                return Conversation.readFromNBT(entry);
            }
        }
        return null;
    }

    /** Sauvegarde une conversation (remplace si existante, ajoute sinon). */
    public static void saveConversation(ItemStack stack, Conversation conv) {
        if (stack.isEmpty()) return;
        CompoundNBT data = get(stack);
        ListNBT list = data.getList("sms", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getString("phoneNumber").equals(conv.getContactPhoneNumber())) {
                list.set(i, conv.writeToNBT());
                data.put("sms", list);
                return;
            }
        }
        list.add(conv.writeToNBT());
        data.put("sms", list);
    }

    /** Retourne la conversation existante ou en cree une nouvelle en memoire (non sauvegardee). */
    public static Conversation getOrCreateConversation(ItemStack stack, String phoneNumber) {
        Conversation conv = findConversation(stack, phoneNumber);
        return conv != null ? conv : new Conversation(phoneNumber);
    }

    // -------------------------------------------------------------------------
    // Fond d'ecran
    // -------------------------------------------------------------------------

    public static String getWallpaper(ItemStack stack) {
        if (stack.isEmpty()) return "wallpaper_default";
        CompoundNBT data = get(stack);
        return data.contains("wallpaper") ? data.getString("wallpaper") : "wallpaper_default";
    }

    public static void setWallpaper(ItemStack stack, String wallpaper) {
        if (stack.isEmpty()) return;
        get(stack).putString("wallpaper", wallpaper);
    }

    // -------------------------------------------------------------------------
    // SMS non lus
    // -------------------------------------------------------------------------

    public static boolean hasUnreadSMS(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return get(stack).getBoolean("hasUnreadSMS");
    }

    public static void setHasUnreadSMS(ItemStack stack, boolean unread) {
        if (stack.isEmpty()) return;
        get(stack).putBoolean("hasUnreadSMS", unread);
    }
}
