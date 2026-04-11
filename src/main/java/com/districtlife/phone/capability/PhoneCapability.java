package com.districtlife.phone.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class PhoneCapability implements IPhoneCapability {

    private List<Contact> contacts = new ArrayList<>();
    private List<Conversation> sms = new ArrayList<>();
    private List<String> photoPaths = new ArrayList<>();
    private String wallpaper = "wallpaper_default";
    private boolean hasUnreadSMS = false;

    @Override
    public List<Contact> getContacts() { return contacts; }

    @Override
    public void addContact(Contact contact) { contacts.add(contact); }

    @Override
    public void removeContact(String uuid) {
        contacts.removeIf(c -> c.getUuid().toString().equals(uuid));
    }

    @Override
    public List<Conversation> getSMS() { return sms; }

    @Override
    public void addConversation(Conversation conversation) { sms.add(conversation); }

    @Override
    public Conversation findConversation(String phoneNumber) {
        for (Conversation c : sms) {
            if (c.getContactPhoneNumber().equals(phoneNumber)) return c;
        }
        return null;
    }

    @Override
    public Conversation getOrCreateConversation(String phoneNumber) {
        Conversation conv = findConversation(phoneNumber);
        if (conv == null) {
            conv = new Conversation(phoneNumber);
            sms.add(conv);
        }
        return conv;
    }

    @Override
    public List<String> getPhotoPaths() { return photoPaths; }

    @Override
    public void addPhotoPath(String path) { photoPaths.add(path); }

    @Override
    public String getWallpaper() { return wallpaper; }

    @Override
    public void setWallpaper(String wallpaper) { this.wallpaper = wallpaper; }

    @Override
    public boolean hasUnreadSMS() { return hasUnreadSMS; }

    @Override
    public void setHasUnreadSMS(boolean unread) { this.hasUnreadSMS = unread; }

    @Override
    public CompoundNBT writeToNBT() {
        CompoundNBT nbt = new CompoundNBT();

        ListNBT contactList = new ListNBT();
        for (Contact c : contacts) contactList.add(c.writeToNBT());
        nbt.put("contacts", contactList);

        ListNBT smsList = new ListNBT();
        for (Conversation conv : sms) smsList.add(conv.writeToNBT());
        nbt.put("sms", smsList);

        ListNBT photoList = new ListNBT();
        for (String path : photoPaths) {
            CompoundNBT entry = new CompoundNBT();
            entry.putString("path", path);
            photoList.add(entry);
        }
        nbt.put("photos", photoList);

        nbt.putString("wallpaper", wallpaper);
        nbt.putBoolean("hasUnreadSMS", hasUnreadSMS);

        return nbt;
    }

    @Override
    public void readFromNBT(CompoundNBT nbt) {
        contacts.clear();
        ListNBT contactList = nbt.getList("contacts", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < contactList.size(); i++) {
            contacts.add(Contact.readFromNBT(contactList.getCompound(i)));
        }

        sms.clear();
        ListNBT smsList = nbt.getList("sms", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < smsList.size(); i++) {
            sms.add(Conversation.readFromNBT(smsList.getCompound(i)));
        }

        photoPaths.clear();
        ListNBT photoList = nbt.getList("photos", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < photoList.size(); i++) {
            photoPaths.add(photoList.getCompound(i).getString("path"));
        }

        wallpaper = nbt.contains("wallpaper") ? nbt.getString("wallpaper") : "wallpaper_default";
        hasUnreadSMS = nbt.getBoolean("hasUnreadSMS");
    }

    @Override
    public void reset() {
        contacts.clear();
        sms.clear();
        photoPaths.clear();
        wallpaper = "wallpaper_default";
        hasUnreadSMS = false;
    }
}
