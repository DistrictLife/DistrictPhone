package com.districtlife.phone.item;

import com.districtlife.phone.block.PhoneFixTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
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

    /** Copie le numero de telephone de l'item vers la TileEntity apres la pose du bloc. */
    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, World level,
                                                  @Nullable PlayerEntity player,
                                                  ItemStack stack, BlockState state) {
        super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (!level.isClientSide()) {
            TileEntity te = level.getBlockEntity(pos);
            if (te instanceof PhoneFixTileEntity) {
                ((PhoneFixTileEntity) te).setPhoneNumber(getPhoneNumber(stack));
            }
        }
        return true;
    }
}
