package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.RitualManager;
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
import java.util.Arrays;
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
            plugin.sendMessage(sender, GlobalMessages.CMD_PLAYER_ONLY);
            return true;
        }

        if (args.length < 1) {
            plugin.sendMessage(player, Messages.RITUAL_CMD_USAGE);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "info" -> handleInfo(player, args);
            case "cancel" -> handleCancel(player);
            default -> {
                plugin.sendMessage(player, Messages.RITUAL_CMD_USAGE);
                yield true;
            }
        };
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

    private boolean handleCancel(Player player) {
        RitualManager ritualManager = plugin.getRitualManager();
        Ritual currentRitual = ritualManager.getCurrentRitualForPlayer(player);

        if (currentRitual == null) {
            plugin.sendMessage(player, Messages.RITUAL_NOT_IN_PROGRESS);
            return true;
        }

        ritualManager.cancelRitualFor(player);
        plugin.sendMessage(player, Messages.RITUAL_CANCELLED.formatted(
                Placeholder.unparsed("ritual", currentRitual.getDisplayName())
        ));

        return true;
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
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
