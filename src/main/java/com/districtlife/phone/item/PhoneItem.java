package com.districtlife.phone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;

public class PhoneItem extends Item {

    private static final String NBT_PHONE_NUMBER = "PhoneNumber";
    private static final Random RANDOM = new Random();

    public PhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) {
            String number = getPhoneNumber(stack);
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.districtlife.phone.screen.PhoneScreen.open(number));
        }
        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public ITextComponent getName(ItemStack stack) {
        return new TranslationTextComponent("item.districtlife_phone.phone");
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

    // -------------------------------------------------------------------------
    // Helpers NBT
    // -------------------------------------------------------------------------

    /** Retourne le numero stocke dans le NBT de l'item, ou "" si absent. */
    public static String getPhoneNumber(ItemStack stack) {
        CompoundNBT nbt = stack.getTag();
        if (nbt != null && nbt.contains(NBT_PHONE_NUMBER)) {
            return nbt.getString(NBT_PHONE_NUMBER);
        }
        return "";
    }

    /** Ecrit le numero dans le NBT de l'item. */
    public static void setPhoneNumber(ItemStack stack, String number) {
        stack.getOrCreateTag().putString(NBT_PHONE_NUMBER, number);
    }

    /** Genere un numero au format "06 XX XX XX XX". */
    public static String generatePhoneNumber() {
        return String.format("06 %02d %02d %02d %02d",
                RANDOM.nextInt(100),
                RANDOM.nextInt(100),
                RANDOM.nextInt(100),
                RANDOM.nextInt(100));
    }

    // -------------------------------------------------------------------------
    // Recherche dans l'inventaire
    // -------------------------------------------------------------------------

    /**
     * Retourne le premier ItemStack telephone ayant ce numero dans l'inventaire,
     * ou ItemStack.EMPTY si introuvable.
     */
    public static ItemStack findPhoneStack(PlayerEntity player, String phoneNumber) {
        for (ItemStack stack : player.inventory.items) {
            if (stack.getItem() instanceof PhoneItem && getPhoneNumber(stack).equals(phoneNumber))
                return stack;
        }
        for (ItemStack stack : player.inventory.offhand) {
            if (stack.getItem() instanceof PhoneItem && getPhoneNumber(stack).equals(phoneNumber))
                return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Retourne le premier telephone avec un numero dans l'inventaire,
     * ou ItemStack.EMPTY si introuvable.
     */
    public static ItemStack findFirstPhoneStack(PlayerEntity player) {
        for (ItemStack stack : player.inventory.items) {
            if (stack.getItem() instanceof PhoneItem && !getPhoneNumber(stack).isEmpty())
                return stack;
        }
        for (ItemStack stack : player.inventory.offhand) {
            if (stack.getItem() instanceof PhoneItem && !getPhoneNumber(stack).isEmpty())
                return stack;
        }
        return ItemStack.EMPTY;
    }
}
