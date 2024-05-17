package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.rituals.RitualManager;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RitualCommand implements CommandExecutor, TabCompleter {

    private final Devotions plugin;

    public RitualCommand(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
        if (!(sender instanceof Player player)) {
            Devotions.sendMessage(sender, GlobalMessages.CMD_PLAYER_ONLY);
            return true;
        }

        if (args.length < 1) {
            Devotions.sendMessage(player, Messages.RITUAL_CMD_USAGE);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "info" -> handleInfo(player, args);
            case "cancel" -> handleCancel(player);
            default -> {
                Devotions.sendMessage(player, Messages.RITUAL_CMD_USAGE);
                yield true;
            }
        };
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            Devotions.sendMessage(player, Messages.RITUAL_CMD_SPECIFY);
            return false;
        }

        String ritualName = args[1];
        Ritual selectedRitual = plugin.getRitualManager().getRitualByKey(ritualName);

        if (selectedRitual == null) {
            Devotions.sendMessage(player, Messages.RITUAL_NOT_FOUND);
            return true;
        }

        // Display ritual information
        displayRitualInfo(player, selectedRitual);

        return true;
    }

    private void displayRitualInfo(Player player, Ritual ritual) {
        Devotions.sendMessage(player, Messages.RITUAL_INFO
                .insertParsed("display-name", ritual.getDisplayName())
                .insertParsed("description", ritual.getDescription())
                .insertString("item-name", ritual.getParsedItemName())
                .insertNumber("favour-amount", ritual.getFavorAmount())
        );
    }

    private boolean handleCancel(Player player) {
        RitualManager ritualManager = plugin.getRitualManager();
        Ritual currentRitual = ritualManager.getCurrentRitualForPlayer(player);

        if (currentRitual == null) {
            Devotions.sendMessage(player, Messages.RITUAL_NOT_IN_PROGRESS);
            return true;
        }

        ritualManager.cancelRitualFor(player);
        Devotions.sendMessage(player, Messages.RITUAL_CANCELLED
                .insertParsed("ritual", currentRitual.getDisplayName())
        );

        return true;
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("info", "cancel"));
        } else if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
            List<String> ritualNames = plugin.getRitualManager().getAllRitualNames();
            suggestions.addAll(ritualNames);
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

}
