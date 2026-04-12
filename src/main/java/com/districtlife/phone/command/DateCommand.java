package com.districtlife.phone.command;

import com.districtlife.phone.util.RPTime;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

/**
 * Commande /date jj mm aaaa
 *
 * Modifie la date RP du monde en recalculant le dayTime de toutes les dimensions.
 * La formule inverse de RPTime :
 *   absoluteDay = (year - 1) * 360 + (month - 1) * 30 + (day - 1)
 *   targetTick  = absoluteDay * 24000 + (currentDayTime % 24000)
 *
 * L'heure actuelle est preservee ; seule la date change.
 * Requiert le niveau de permission 2 (operateur).
 */
public class DateCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("date")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("jour", IntegerArgumentType.integer(1, 30))
                    .then(Commands.argument("mois", IntegerArgumentType.integer(1, 12))
                        .then(Commands.argument("annee", IntegerArgumentType.integer(1, 9999))
                            .executes(ctx -> setDate(
                                    ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "jour"),
                                    IntegerArgumentType.getInteger(ctx, "mois"),
                                    IntegerArgumentType.getInteger(ctx, "annee")
                            )))))
        );
    }

    private static int setDate(CommandSource source, int day, int month, int year) {
        ServerWorld overworld = source.getServer().overworld();

        // Jour absolu cible (base 0)
        long targetAbsoluteDay = (long) (year - 1) * 360
                               + (long) (month - 1) * 30
                               + (long) (day   - 1);

        // Preservation de l'heure en cours
        long tickOfDay = overworld.getDayTime() % 24000;

        long targetTick = targetAbsoluteDay * 24000 + tickOfDay;

        // Application sur toutes les dimensions (Overworld, Nether, End)
        for (ServerWorld world : source.getServer().getAllLevels()) {
            world.setDayTime(targetTick);
        }

        // Confirmation avec la date RP formatee
        RPTime rpTime = new RPTime(targetTick);
        source.sendSuccess(
                new StringTextComponent(
                        "\u00A7aDate definie : \u00A7f"
                        + rpTime.getDayName() + " "
                        + rpTime.getDayOfMonth() + " "
                        + rpTime.getMonthName()
                        + " - An " + rpTime.getYear()),
                true);
        return 1;
    }
}
