package com.districtlife.phone.capability;

import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

public class Contact {

    private UUID uuid;
    private String pseudo;
    private String phoneNumber; // numero RP optionnel

    public Contact(UUID uuid, String pseudo, String phoneNumber) {
        this.uuid = uuid;
        this.pseudo = pseudo;
        this.phoneNumber = phoneNumber;
    }

    public UUID getUuid() { return uuid; }
    public String getPseudo() { return pseudo; }
    public String getPhoneNumber() { return phoneNumber; }

    public CompoundNBT writeToNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putUUID("uuid", uuid);
        nbt.putString("pseudo", pseudo);
        nbt.putString("phoneNumber", phoneNumber);
        return nbt;
    }

    public static Contact readFromNBT(CompoundNBT nbt) {
        UUID uuid = nbt.getUUID("uuid");
        String pseudo = nbt.getString("pseudo");
        String phoneNumber = nbt.getString("phoneNumber");
        return new Contact(uuid, pseudo, phoneNumber);
    }
}
