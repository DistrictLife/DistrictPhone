package com.districtlife.phone.command;

import com.districtlife.phone.data.PhoneData;
import com.districtlife.phone.dynmap.DynmapConfig;
import com.districtlife.phone.dynmap.MapPoint;
import com.districtlife.phone.dynmap.MapPointsData;
import com.districtlife.phone.item.PhoneFixItem;
import com.districtlife.phone.item.PhoneItem;
import com.districtlife.phone.network.PacketHandler;
import com.districtlife.phone.network.PacketOpenDebugTexture;
import com.districtlife.phone.network.PacketReceiveNews;
import com.districtlife.phone.network.PacketSyncDynmap;
import com.districtlife.phone.network.PacketSyncNews;
import com.districtlife.phone.network.PacketSyncMapPoints;
import com.districtlife.phone.network.PacketSyncPhone;
import com.districtlife.phone.news.NewsArticle;
import com.districtlife.phone.news.NewsManager;
import com.districtlife.phone.registry.ModBlocks;
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
        registerDebugPhone(event.getDispatcher());
        registerPhoneFix(event.getDispatcher());
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
                .then(Commands.literal("map-point")
                    .then(Commands.argument("color", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> addMapPoint(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "color"),
                                    StringArgumentType.getString(ctx, "name"))))))
        );
    }

    // -------------------------------------------------------------------------
    // /debug-phone <texture>
    // -------------------------------------------------------------------------

    private static void registerDebugPhone(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("debug-phone")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("texture", StringArgumentType.greedyString())
                    .executes(ctx -> openDebugTexture(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "texture"))))
        );
    }

    private static int openDebugTexture(CommandSource source, String texturePath)
            throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        PacketHandler.sendToPlayer(new PacketOpenDebugTexture(texturePath.trim()), player);
        source.sendSuccess(
                new StringTextComponent("§7[debug-phone] Ouverture : §f" + texturePath.trim()),
                false);
        return 1;
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
                .then(Commands.literal("remove")
                    .then(Commands.argument("cible", StringArgumentType.greedyString())
                        .executes(ctx -> removeNews(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "cible")))))
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
    // /news remove <all | N | titre>
    // -------------------------------------------------------------------------

    private static int removeNews(CommandSource source, String target) {
        int removed;
        String feedback;

        String trimmed = target.trim();

        if (trimmed.equalsIgnoreCase("all")) {
            removed  = NewsManager.removeAll();
            feedback = removed == 0
                    ? "\u00A7eAucun article a supprimer."
                    : "\u00A7a" + removed + " article(s) supprime(s).";

        } else {
            // Essaie de parser un nombre
            Integer n = tryParsePositiveInt(trimmed);
            if (n != null) {
                removed  = NewsManager.removeRecent(n);
                feedback = removed == 0
                        ? "\u00A7eAucun article a supprimer."
                        : "\u00A7a" + removed + " article(s) recent(s) supprime(s).";
            } else {
                // Recherche par titre
                removed  = NewsManager.removeByTitle(trimmed);
                feedback = removed == 0
                        ? "\u00A7eAucun article dont le titre contient \"\u00A7f" + trimmed + "\u00A7e\"."
                        : "\u00A7a" + removed + " article(s) supprime(s) (titre contient \"\u00A7f" + trimmed + "\u00A7a\").";
            }
        }

        // Synchronise la liste mise a jour a tous les joueurs
        PacketSyncNews syncPkt = new PacketSyncNews(new java.util.ArrayList<>(NewsManager.getAll()));
        for (ServerPlayerEntity player : source.getServer().getPlayerList().getPlayers()) {
            PacketHandler.sendToPlayer(syncPkt, player);
        }

        source.sendSuccess(new StringTextComponent(feedback), true);
        return removed > 0 ? 1 : 0;
    }

    /** Retourne l'entier si la chaine est un entier strictement positif, null sinon. */
    private static Integer tryParsePositiveInt(String s) {
        try {
            int v = Integer.parseInt(s);
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
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

    // -------------------------------------------------------------------------
    // /phone map-point <color> <name>
    // -------------------------------------------------------------------------

    private static int addMapPoint(CommandSource source, String colorStr, String name)
            throws CommandSyntaxException {
        // Recupere la position du joueur executant la commande
        ServerPlayerEntity player = source.getPlayerOrException();
        int wx = (int) Math.floor(player.getX());
        int wz = (int) Math.floor(player.getZ());

        // Parse la couleur hex (#RRGGBB ou RRGGBB)
        String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
        int color;
        try {
            color = 0xFF000000 | (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            source.sendFailure(new StringTextComponent(
                    "\u00A7cCouleur invalide. Exemples : #FF0000, 00FF00, 0000FF"));
            return 0;
        }

        MapPoint point = new MapPoint(name, color, wx, wz);
        MapPointsData data = MapPointsData.get(source.getServer());
        data.addPoint(point);

        // Diffuse la liste mise a jour a tous les joueurs
        PacketSyncMapPoints pkt = new PacketSyncMapPoints(data.getPoints());
        for (ServerPlayerEntity p : source.getServer().getPlayerList().getPlayers()) {
            PacketHandler.sendToPlayer(pkt, p);
        }

        source.sendSuccess(new StringTextComponent(
                "\u00A7aPoint \"\u00A7f" + name + "\u00A7a\" ajoute en (" + wx + ", " + wz + ")."),
                true);
        return 1;
    }

    // -------------------------------------------------------------------------
    // /phone-fix give <numero> <joueur>
    // -------------------------------------------------------------------------

    private static void registerPhoneFix(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("phone-fix")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("give")
                    .then(Commands.argument("numero", StringArgumentType.string())
                        .then(Commands.argument("joueur", EntityArgument.player())
                            .executes(ctx -> givePhoneFix(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "numero"),
                                    EntityArgument.getPlayer(ctx, "joueur"))))))
        );
    }

    private static int givePhoneFix(CommandSource source, String numero, ServerPlayerEntity target)
            throws CommandSyntaxException {
        String digits = numero.replaceAll("[^0-9]", "");
        if (digits.length() < 2 || digits.length() > 10) {
            source.sendFailure(new StringTextComponent(
                    "\u00A7cNumero invalide. Il doit contenir entre 2 et 10 chiffres (saisi : "
                            + digits.length() + ")."));
            return 0;
        }

        // Formate comme PhoneItem.generatePhoneNumber() : "06 XX XX XX XX"
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 2 == 0) sb.append(' ');
            sb.append(digits.charAt(i));
        }
        String formatted = sb.toString();

        ItemStack stack = new ItemStack(ModBlocks.PHONE_FIX_ITEM.get());
        PhoneFixItem.setPhoneNumber(stack, formatted);

        target.inventory.add(stack);
        source.sendSuccess(
                new StringTextComponent("§aBoitier telephonique donne a §f"
                        + target.getDisplayName().getString()
                        + "§a (numero : §f" + formatted + "§a)."),
                true);
        return 1;
    }
}