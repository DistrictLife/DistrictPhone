package com.districtlife.phone.item;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class PhoneFixItem extends BlockItem {

    private static final String NBT_PHONE_NUMBER = "PhoneNumber";

    public PhoneFixItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world,
                                List<ITextComponent> tooltip, ITooltipFlag flag) {
        String number = getPhoneNumber(stack);
        if (!number.isEmpty()) {
            tooltip.add(new StringTextComponent(number)
                    .withStyle(TextFormatting.GRAY));
        }
    }

    public static String getPhoneNumber(ItemStack stack) {
        CompoundNBT nbt = stack.getTag();
        return (nbt != null && nbt.contains(NBT_PHONE_NUMBER)) ? nbt.getString(NBT_PHONE_NUMBER) : "";
    }

    public static void setPhoneNumber(ItemStack stack, String number) {
        stack.getOrCreateTag().putString(NBT_PHONE_NUMBER, number);
    }
}
