package com.districtlife.phone.capability;

import net.minecraft.nbt.CompoundNBT;

import java.util.List;

public interface IPhoneCapability {

    // --- Contacts ---
    List<Contact> getContacts();
    void addContact(Contact contact);
    void removeContact(String uuid);

    // --- SMS ---
    List<Conversation> getSMS();
    void addConversation(Conversation conversation);
    /** Retourne la conversation avec ce numero, ou null si elle n'existe pas. */
    Conversation findConversation(String phoneNumber);
    /** Retourne la conversation avec ce numero, en la creant si necessaire. */
    Conversation getOrCreateConversation(String phoneNumber);

    // --- Photos ---
    List<String> getPhotoPaths();
    void addPhotoPath(String path);

    // --- Parametres ---
    String getWallpaper();
    void setWallpaper(String wallpaper);

    boolean hasUnreadSMS();
    void setHasUnreadSMS(boolean unread);

    // --- Persistence ---
    CompoundNBT writeToNBT();
    void readFromNBT(CompoundNBT nbt);
    void reset();
}
