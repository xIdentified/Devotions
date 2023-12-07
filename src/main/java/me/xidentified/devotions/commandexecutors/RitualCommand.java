package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.util.MessageUtils;
import me.xidentified.devotions.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RitualCommand implements CommandExecutor, TabCompleter {
    private final Devotions plugin;

    public RitualCommand(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Devotions.getInstance().sendMessage(sender, Messages.GENERAL_CMD_PLAYER_ONLY);
            return true;
        }

        if (args.length < 1) {
            plugin.sendMessage(player, "<yellow>Usage: /ritual <info> [RitualName]");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("info")) {
            return handleInfo(player, args);
        }
        plugin.sendMessage(player, "<yellow>Usage: /ritual <info> [RitualName]");
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(player, "<yellow>Please specify the ritual you'd like to lookup information for.");
            return false;
        }

        String ritualName = args[1];
        Ritual selectedRitual = plugin.getRitualManager().getRitualByKey(ritualName);

        if (selectedRitual == null) {
            plugin.sendMessage(player, "<red>Unknown ritual. Please choose a valid ritual name.");
            return true;
        }

        // Display ritual information
        displayRitualInfo(player, selectedRitual);

        return true;
    }

    private void displayRitualInfo(Player player, Ritual ritual) {
        // TODO one message
        plugin.sendMessage(player, "<gold>Details of " + ritual.getDisplayName());
        plugin.sendMessage(player, "<yellow>Description: <gray>" + ritual.getDescription());
        plugin.sendMessage(player, "<yellow>Key Item: <gray>" + ritual.getParsedItemName());
        plugin.sendMessage(player, "<yellow>Favor Rewarded: <gray>" + ritual.getFavorAmount());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("info");
        } else if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
            // Suggestions for the second argument (e.g., ritual names)
            List<String> ritualNames = plugin.getRitualManager().getAllRitualNames();
            suggestions.addAll(ritualNames);
        }

        // Filter based on what the player has already typed
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
