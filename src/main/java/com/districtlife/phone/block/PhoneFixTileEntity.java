package com.districtlife.phone.block;

import com.districtlife.phone.call.CallManager;
import com.districtlife.phone.registry.ModSounds;
import com.districtlife.phone.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;

import java.util.UUID;

public class PhoneFixTileEntity extends TileEntity implements ITickableTileEntity {

    private String phoneNumber   = "";
    private String pendingCaller = "";
    private String activeCall    = "";
    private UUID   interactingPlayer = null;

    private int ringTimer = 0;

    public PhoneFixTileEntity() {
        super(ModTileEntities.PHONE_FIX.get());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getPhoneNumber()   { return phoneNumber; }
    public String getPendingCaller() { return pendingCaller; }
    public String getActiveCall()    { return activeCall; }
    public UUID   getInteractingPlayer() { return interactingPlayer; }

    public boolean isIdle()    { return pendingCaller.isEmpty() && activeCall.isEmpty(); }
    public boolean isRinging() { return !pendingCaller.isEmpty(); }
    public boolean isInCall()  { return !activeCall.isEmpty(); }

    // -------------------------------------------------------------------------
    // State changes (server-side)
    // -------------------------------------------------------------------------

    public void setPhoneNumber(String number) {
        this.phoneNumber = number;
        markDirtyAndSync();
        if (level != null && !level.isClientSide()) {
            CallManager.registerFix(number, this);
        }
    }

    public void setRinging(String callerPhone) {
        this.pendingCaller = callerPhone;
        this.activeCall    = "";
        this.ringTimer     = 0;
        markDirtyAndSync();
    }

    public void setInCall(String partnerPhone, UUID interactor) {
        this.pendingCaller      = "";
        this.activeCall         = partnerPhone;
        this.interactingPlayer  = interactor;
        this.ringTimer          = 0;
        markDirtyAndSync();
    }

    public void resetCallState() {
        this.pendingCaller     = "";
        this.activeCall        = "";
        this.interactingPlayer = null;
        this.ringTimer         = 0;
        markDirtyAndSync();
    }

    public void setInteractingPlayer(UUID uuid) {
        this.interactingPlayer = uuid;
    }

    private void markDirtyAndSync() {
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // -------------------------------------------------------------------------
    // Tick : sonnerie du boitier
    // -------------------------------------------------------------------------

    @Override
    public void tick() {
        if (level == null || level.isClientSide()) return;
        if (isRinging()) {
            if (ringTimer % 80 == 0) {
                level.playSound(null, worldPosition,
                        ModSounds.PHONE_RING_FIX.get(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
            ringTimer++;
        } else {
            ringTimer = 0;
        }
    }

    // -------------------------------------------------------------------------
    // Cycle de vie TileEntity
    // -------------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && !phoneNumber.isEmpty()) {
            CallManager.registerFix(phoneNumber, this);
            // Nettoie tout etat d'appel perime apres un redemarrage serveur
            if (isRinging() || isInCall()) {
                pendingCaller    = "";
                activeCall       = "";
                interactingPlayer = null;
                this.setChanged();
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (!phoneNumber.isEmpty()) {
            CallManager.unregisterFix(phoneNumber);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!phoneNumber.isEmpty()) {
            CallManager.unregisterFix(phoneNumber);
        }
    }

    // -------------------------------------------------------------------------
    // NBT
    // -------------------------------------------------------------------------

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putString("PhoneNumber",   phoneNumber);
        nbt.putString("PendingCaller", pendingCaller);
        nbt.putString("ActiveCall",    activeCall);
        if (interactingPlayer != null) {
            nbt.putUUID("InteractingPlayer", interactingPlayer);
        }
        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        phoneNumber   = nbt.getString("PhoneNumber");
        pendingCaller = nbt.getString("PendingCaller");
        activeCall    = nbt.getString("ActiveCall");
        interactingPlayer = nbt.hasUUID("InteractingPlayer") ? nbt.getUUID("InteractingPlayer") : null;
    }

    // -------------------------------------------------------------------------
    // Sync client
    // -------------------------------------------------------------------------

    @Override
    public CompoundNBT getUpdateTag() { return save(new CompoundNBT()); }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(worldPosition, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        load(getBlockState(), pkt.getTag());
    }
}
