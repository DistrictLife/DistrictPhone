package com.districtlife.phone.command;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.item.PhoneItem;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketSyncPhone;
import com.districtlife.phone.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;

public class PhoneCommand {

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("phone")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("give")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> givePhone(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> resetPhone(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player")))))
        );
    }

    private static int givePhone(CommandSource source, ServerPlayerEntity target)
            throws CommandSyntaxException {
        ItemStack phoneStack = new ItemStack(ModItems.PHONE.get());
        String number = PhoneItem.generatePhoneNumber();
        PhoneItem.setPhoneNumber(phoneStack, number);

        target.inventory.add(phoneStack);
        source.sendSuccess(
                new TranslationTextComponent("command.districtlife_phone.give.success",
                        target.getDisplayName(),
                        new StringTextComponent(number)),
                true);
        return 1;
    }

    private static int resetPhone(CommandSource source, ServerPlayerEntity target)
            throws CommandSyntaxException {
        // Efface les donnees de tous les telephones dans l'inventaire du joueur
        for (ItemStack stack : target.inventory.items) {
            if (stack.getItem() instanceof PhoneItem) {
                PhoneData.setRaw(stack, new CompoundNBT());
                String phone = PhoneItem.getPhoneNumber(stack);
                if (!phone.isEmpty()) {
                    PacketHandler.sendToPlayer(
                            new PacketSyncPhone(phone, PhoneData.getRaw(stack)), target);
                }
            }
        }
        target.inventory.setChanged();
        source.sendSuccess(
                new TranslationTextComponent("command.districtlife_phone.reset.success",
                        target.getDisplayName()),
                true);
        return 1;
    }
}
