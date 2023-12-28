package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.util.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
            plugin.sendMessage(player, Messages.RITUAL_CMD_USAGE);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("info")) {
            return handleInfo(player, args);
        }
        plugin.sendMessage(player, Messages.RITUAL_CMD_USAGE);
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(player, Messages.RITUAL_CMD_SPECIFY);
            return false;
        }

        String ritualName = args[1];
        Ritual selectedRitual = plugin.getRitualManager().getRitualByKey(ritualName);

        if (selectedRitual == null) {
            plugin.sendMessage(player, Messages.RITUAL_NOT_FOUND);
            return true;
        }

        // Display ritual information
        displayRitualInfo(player, selectedRitual);

        return true;
    }

    private void displayRitualInfo(Player player, Ritual ritual) {
        plugin.sendMessage(player, Messages.RITUAL_INFO.formatted(
            Placeholder.unparsed("display-name", ritual.getDisplayName()),
            Placeholder.unparsed("description", ritual.getDescription()),
            Placeholder.unparsed("item-name", ritual.getParsedItemName()),
            Formatter.number("favour-amount", ritual.getFavorAmount())
        ));
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
