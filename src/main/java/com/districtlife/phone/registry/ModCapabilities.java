package com.districtlife.phone.registry;

import com.districtlife.phone.capability.IPhoneCapability;
import com.districtlife.phone.capability.PhoneCapability;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.annotation.Nullable;

public class ModCapabilities {

    @CapabilityInject(IPhoneCapability.class)
    public static Capability<IPhoneCapability> PHONE_CAPABILITY = null;

    public static void register(IEventBus modBus) {
        modBus.addListener(ModCapabilities::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CapabilityManager.INSTANCE.register(
                IPhoneCapability.class,
                new Capability.IStorage<IPhoneCapability>() {
                    @Nullable
                    @Override
                    public INBT writeNBT(Capability<IPhoneCapability> capability,
                                         IPhoneCapability instance, Direction side) {
                        return instance.writeToNBT();
                    }

                    @Override
                    public void readNBT(Capability<IPhoneCapability> capability,
                                        IPhoneCapability instance, Direction side, INBT nbt) {
                        if (nbt instanceof net.minecraft.nbt.CompoundNBT) {
                            instance.readFromNBT((net.minecraft.nbt.CompoundNBT) nbt);
                        }
                    }
                },
                PhoneCapability::new
        ));
    }
}
