package com.districtlife.phone.command;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.dynmap.DynmapConfig;
import com.districtlife.phone.item.PhoneItem;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketReceiveNews;
import com.districtlife.phone.network.PacketSyncDynmap;
import com.districtlife.phone.network.PacketSyncPhone;
import com.districtlife.phone.news.NewsArticle;
import com.districtlife.phone.news.NewsManager;
import com.districtlife.phone.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
        registerNews(event.getDispatcher());
        DateCommand.register(event.getDispatcher());
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
                .then(Commands.literal("map")
                    .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(ctx -> setMapUrl(ctx.getSource(),
                                StringArgumentType.getString(ctx, "url"))))
                    .executes(ctx -> clearMapUrl(ctx.getSource())))
        );
    }

    // -------------------------------------------------------------------------
    // /news publish <titre> | <contenu>
    // -------------------------------------------------------------------------

    private static void registerNews(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("news")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("publish")
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> publishNews(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "text")))))
        );
    }

    private static int publishNews(CommandSource source, String text) {
        String[] parts = text.split("\\|", 2);
        if (parts.length < 2) {
            source.sendFailure(new StringTextComponent(
                    "Usage : /news publish <titre> | <contenu>"));
            return 0;
        }
        String title   = parts[0].trim();
        String content = parts[1].trim();
        if (title.isEmpty() || content.isEmpty()) {
            source.sendFailure(new StringTextComponent("Titre et contenu ne peuvent pas etre vides."));
            return 0;
        }

        String author;
        try {
            author = source.getPlayerOrException().getDisplayName().getString();
        } catch (CommandSyntaxException e) {
            author = source.getTextName();
        }

        long tickRP = source.getServer().overworld().getGameTime();
        NewsArticle article = NewsManager.publish(title, author, content, tickRP);

        // Broadcast a tous les joueurs connectes
        for (ServerPlayerEntity player : source.getServer().getPlayerList().getPlayers()) {
            PacketHandler.sendToPlayer(new PacketReceiveNews(article), player);
        }

        source.sendSuccess(
                new StringTextComponent("Article publie (#" + article.id + ") : " + title),
                true);
        return 1;
    }

    // -------------------------------------------------------------------------
    // /phone give | reset
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // /phone map <url>  |  /phone map  (efface)
    // -------------------------------------------------------------------------

    private static int setMapUrl(CommandSource source, String url) {
        DynmapConfig.get(source.getServer()).setBaseUrl(url);

        // Diffuse a tous les joueurs connectes
        PacketSyncDynmap pkt = new PacketSyncDynmap(url);
        for (ServerPlayerEntity player : source.getServer().getPlayerList().getPlayers()) {
            PacketHandler.sendToPlayer(pkt, player);
        }

        source.sendSuccess(
                new StringTextComponent("\u00A7aURL Dynmap definie : \u00A7f" + url),
                true);
        return 1;
    }

    private static int clearMapUrl(CommandSource source) {
        DynmapConfig.get(source.getServer()).setBaseUrl("");

        PacketSyncDynmap pkt = new PacketSyncDynmap("");
        for (ServerPlayerEntity player : source.getServer().getPlayerList().getPlayers()) {
            PacketHandler.sendToPlayer(pkt, player);
        }

        source.sendSuccess(
                new StringTextComponent("\u00A7eURL Dynmap effacee (carte statique)."),
                true);
        return 1;
    }
}
