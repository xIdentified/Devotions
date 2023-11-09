package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeityCommand implements CommandExecutor, TabCompleter {

    private final Devotions plugin;

    public DeityCommand(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.parse("<red>This command can only be used by players!"));
            return true;
        }

        if (args.length < 1) {
            sendMessage(player,"<yellow>Usage: /deity <select|info> [DeityName]");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "list" -> {
                return handleList(player);
            }
            case "select" -> {
                return handleSelect(player, args);
            }
            case "info" -> {
                return handleInfo(player, args);
            }
            default -> {
                sendMessage(player,"<yellow>Usage: /deity <select|info> [DeityName]");
                return true;
            }
        }
    }

    private boolean handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "<yellow>Please specify the deity you wish to worship.");
            return true;
        }

        String deityName = args[1];
        Deity selectedDeity = plugin.getDevotionManager().getDeityByName(deityName);

        if (selectedDeity == null) {
            sendMessage(player, "<red>Unknown deity. Please choose a valid deity name.");
            return true;
        }

        // Fetch or create the player's FavorManager
        UUID playerUniqueId = player.getUniqueId();
        FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(playerUniqueId);

        if (favorManager == null) {
            // Create a new FavorManager if the player doesn't have one
            favorManager = new FavorManager(plugin, playerUniqueId, selectedDeity);
        } else {
            // Update the existing FavorManager with the new deity
            favorManager.setDeity(selectedDeity);
        }

        // Set the player's devotion with the updated FavorManager
        plugin.getDevotionManager().setPlayerDevotion(playerUniqueId, favorManager);

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player,"<yellow>Please specify the deity whose information you wish to view.");
            return true;
        }

        String deityName = args[1];
        Deity selectedDeity = plugin.getDevotionManager().getDeityByName(deityName);

        if (selectedDeity == null) {
            sendMessage(player,"<red>Unknown deity. Please choose a valid deity name.");
            return true;
        }

        // Display deity information
        sendMessage(player,"<gold>Details of " + selectedDeity.getName());
        sendMessage(player,"<yellow>Lore: <gray>" + selectedDeity.getLore());
        sendMessage(player,"<yellow>Domain: <gray>" + String.join(", ", selectedDeity.getDomain()));
        sendMessage(player,"<yellow>Alignment: <gray>" + selectedDeity.getAlignment());
        sendMessage(player,"<yellow>Favored Rituals: <gray>" + selectedDeity.getRituals());
        sendMessage(player,"<yellow>Favored Offerings: <gray>" + selectedDeity.getOfferings());

        return true;
    }

    private boolean handleList(Player player) {
        List<Deity> deities = plugin.getDevotionManager().getAllDeities();
        if (deities.isEmpty()) {
            sendMessage(player,"<red>No deities found.");
            return true;
        }

        sendMessage(player, "<gold>Available Deities:");
        for (Deity deity : deities) {
            player.sendMessage(MessageUtils.parse("<gray>- " + deity.getName()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("select");
            completions.add("info");
            completions.add("list");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("info"))) {
            for (Deity deity : plugin.getDevotionManager().getAllDeities()) {
                completions.add(deity.getName());
            }
        }

        return completions;
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(MessageUtils.parse(message));
    }

}
