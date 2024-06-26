package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import me.clip.placeholderapi.PlaceholderAPI;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.util.JavaScriptEngine;
import me.xidentified.devotions.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DeityCommand implements CommandExecutor, TabCompleter {

    private final Devotions plugin;

    public DeityCommand(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
        if (!(sender instanceof Player player)) {
            Devotions.sendMessage(sender, GlobalMessages.CMD_PLAYER_ONLY);
            return true;
        }

        if (!player.hasPermission("devotions.select")) {
            Devotions.sendMessage(player, GlobalMessages.NO_PERM_CMD);
            return true;
        }

        if (args.length == 0) {
            if (displayExistingDeityInfo(player)) {
                return true;
            }
        }

        if (args.length < 1) {
            Devotions.sendMessage(player, Messages.DEITY_CMD_USAGE);
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
            case "abandon" -> {
                return handleAbandon(player);
            }
            default -> {
                Devotions.sendMessage(player, Messages.DEITY_CMD_USAGE);
                return true;
            }
        }
    }

    private boolean handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            return displayExistingDeityInfo(player);
        }

        String deityName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.debugLog("Value of deityName: " + deityName);

        Deity selectedDeity = plugin.getDevotionManager().getDeityByName(deityName);

        if (selectedDeity == null) {
            Devotions.sendMessage(player, Messages.DEITY_NOT_FOUND);
            return true;
        }

        UUID playerUniqueId = player.getUniqueId();
        DevotionManager devotionManager = plugin.getDevotionManager();

        // Check the selection condition
        if (selectedDeity.getSelectionCondition() != null) {
            String condition = PlaceholderAPI.setPlaceholders(player, selectedDeity.getSelectionCondition());
            boolean conditionMet = JavaScriptEngine.evaluateExpression(condition);

            if (!conditionMet) {
                Devotions.sendMessage(player, Messages.SELECTION_CONDITION_NOT_MET
                        .insertParsed("deity", selectedDeity.getName()));
                return true;
            }
        }

        plugin.debugLog("Current devotion status for player " + player.getName() + ": " +
                devotionManager.getPlayerDevotion(playerUniqueId));

        // Check if the player already has a devotion
        FavorManager currentFavorManager = devotionManager.getPlayerDevotion(playerUniqueId);
        if (currentFavorManager != null) {
            // Player has an existing devotion
            if (!currentFavorManager.getDeity().equals(selectedDeity)) {
                // Player is switching to a new deity, so reset the favor and update the deity
                currentFavorManager.resetFavor();
                currentFavorManager.setDeity(selectedDeity);
                // Save the updated devotion
                devotionManager.setPlayerDevotion(playerUniqueId, currentFavorManager);
            } else {
                // Player selected the same deity they're already devoted to
                Devotions.sendMessage(player, Messages.DEVOTION_ALREADY_SET
                        .insertParsed("deity", selectedDeity.getName()));
            }
        } else {
            // Player does not have an existing devotion, create a new FavorManager
            FavorManager newFavorManager = new FavorManager(plugin, playerUniqueId, selectedDeity);
            devotionManager.setPlayerDevotion(playerUniqueId, newFavorManager);
        }

        plugin.debugLog("Updated devotion status for player " + player.getName() + ": " +
                devotionManager.getPlayerDevotion(playerUniqueId));
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            if (displayExistingDeityInfo(player)) {
                return true;
            }
        }

        String deityName = args[1];
        Deity selectedDeity = plugin.getDevotionManager().getDeityByName(deityName);

        if (selectedDeity == null) {
            Devotions.sendMessage(player, Messages.DEITY_NOT_FOUND);
            return false;
        }

        // Display deity information
        Devotions.sendMessage(player, Messages.DEITY_INFO
                .insertParsed("name", selectedDeity.getName())
                .insertString("lore", selectedDeity.getLore())
                .insertString("domain", String.join(", ", selectedDeity.getDomain()))
                .insertString("alignment", selectedDeity.getAlignment())
                .insertString("rituals", selectedDeity.getRituals())
                .insertString("offerings", selectedDeity.getFormattedOfferings())
        );
        return true;
    }

    private boolean displayExistingDeityInfo(Player player) {
        UUID playerUUID = player.getUniqueId();
        FavorManager playerDevotion = plugin.getDevotionManager().getPlayerDevotion(playerUUID);

        if (playerDevotion == null) {
            Devotions.sendMessage(player, Messages.NO_DEVOTION_SET);
            return true;
        }

        Deity deity = playerDevotion.getDeity();
        handleInfo(player, new String[]{"info", deity.name});
        return true;

    }

    private boolean handleList(Player player) {
        List<Deity> deities = plugin.getDevotionManager().getAllDeities();
        if (deities.isEmpty()) {
            Devotions.sendMessage(player, Messages.DEITY_NO_DEITY_FOUND);
            return true;
        }

        Devotions.sendMessage(player, Messages.DEITY_LIST_HEADER);
        for (Deity deity : deities) {
            Devotions.sendMessage(player, Messages.DEITY_LIST_ENTRY
                    .insertParsed("name", deity.name)
            );
        }

        return true;
    }

    private boolean handleAbandon(Player player) {
        UUID playerUniqueId = player.getUniqueId();
        DevotionManager devotionManager = plugin.getDevotionManager();

        // Check if the player has a devotion
        FavorManager favorManager = devotionManager.getPlayerDevotion(playerUniqueId);

        if (favorManager == null) {
            Devotions.sendMessage(player, Messages.NO_DEVOTION_SET);
            return true;
        }

        Deity deity = favorManager.getDeity();
        if (deity != null && deity.getAbandonCondition() != null) {
            String condition = PlaceholderAPI.setPlaceholders(player, deity.getAbandonCondition());
            boolean conditionMet = JavaScriptEngine.evaluateExpression(condition);

            if (!conditionMet) {
                Devotions.sendMessage(player, Messages.ABANDON_CONDITION_NOT_MET
                        .insertParsed("deity", deity.getName())
                );
                return true;
            }
        }

        // Check if reset-favor-on-abandon is true
        boolean resetFavorOnAbandon = plugin.getDevotionsConfig().resetFavorOnAbandon();
        if (resetFavorOnAbandon) {
            favorManager.resetFavor();
        }

        // Remove the player's devotion
        devotionManager.removeDevotion(playerUniqueId);
        Devotions.sendMessage(player, Messages.DEVOTION_ABANDONED);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("select");
            completions.add("info");
            completions.add("list");
            completions.add("abandon");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("info"))) {
            for (Deity deity : plugin.getDevotionManager().getAllDeities()) {
                completions.add(deity.getName());
            }
        }

        return completions;
    }

}
