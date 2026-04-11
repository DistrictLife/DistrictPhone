package com.districtlife.phone.capability;

import com.districtlife.phone.registry.ModCapabilities;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PhoneCapabilityProvider implements ICapabilitySerializable<CompoundNBT> {

    private final IPhoneCapability instance = new PhoneCapability();
    private final LazyOptional<IPhoneCapability> lazyOptional = LazyOptional.of(() -> instance);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.PHONE_CAPABILITY) {
            return lazyOptional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        return instance.writeToNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        instance.readFromNBT(nbt);
    }

    public void invalidate() {
        lazyOptional.invalidate();
    }
}
