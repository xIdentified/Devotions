package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import me.clip.placeholderapi.PlaceholderAPI;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.CooldownManager;
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

        String deityInput = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.debugLog("Value of deityInput: " + deityInput);

        Deity selectedDeity = plugin.getDevotionManager().getDeityByInput(deityInput);

        if (selectedDeity == null) {
            String availableDeities = plugin.getDevotionManager().getAllDeities().stream()
                    .map(Deity::getName)
                    .collect(Collectors.joining(", "));
            Devotions.sendMessage(player, Messages.DEITY_NOT_FOUND
                    .insertParsed("input", deityInput)
                    .insertParsed("deity_list", availableDeities));
            return true;
        }

        UUID playerUniqueId = player.getUniqueId();
        DevotionManager devotionManager = plugin.getDevotionManager();

        // Check if the player already has a devotion
        FavorManager currentFavorManager = devotionManager.getPlayerDevotion(playerUniqueId);
        if (currentFavorManager != null) {
            // Player already has a devotion
            Deity currentDeity = currentFavorManager.getDeity();
            Devotions.sendMessage(player, Messages.DEVOTION_MUST_ABANDON
                    .insertParsed("current_deity", currentDeity.getName())
            );
            return true;
        }

        // Player does not have an existing devotion
        FavorManager newFavorManager = new FavorManager(plugin, playerUniqueId, selectedDeity);

        // Apply repeat-favor logic if the deity was previously abandoned
        int favor = devotionManager.hasAbandonedDeity(playerUniqueId, selectedDeity.getName())
                ? plugin.getConfig().getInt("reselect-favor", 0) // Use reselect-favor for abandoned deities
                : plugin.getConfig().getInt("initial-favor", 100); // Use initial-favor otherwise
        newFavorManager.setFavor(favor);

        // If penalized, clear the abandoned status for this deity
        if (favor == plugin.getConfig().getInt("reselect-favor", 0)) {
            devotionManager.clearAbandonedDeity(playerUniqueId, selectedDeity.getName());
        }

        // Update devotion
        devotionManager.setPlayerDevotion(playerUniqueId, newFavorManager);

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

        String deityInput = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Deity selectedDeity = plugin.getDevotionManager().getDeityByInput(deityInput);

        if (selectedDeity == null) {
            String availableDeities = plugin.getDevotionManager().getAllDeities().stream()
                    .map(Deity::getName)
                    .collect(Collectors.joining(", "));
            Devotions.sendMessage(player, Messages.DEITY_NOT_FOUND
                    .insertParsed("input", deityInput)
                    .insertParsed("deity_list", availableDeities));
            return false;
        }

        // Display deity information
        Devotions.sendMessage(player, Messages.DEITY_INFO
                .insertParsed("name", selectedDeity.getName())
                .insertString("lore", selectedDeity.getLore())
                .insertString("domain", String.join(", ", selectedDeity.getDomain()))
                .insertString("alignment", selectedDeity.getAlignment())
                .insertString("rituals", String.join(", ", selectedDeity.getRituals()))
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
        CooldownManager cooldownManager = plugin.getCooldownManager();

        // Check if the player is still on cooldown
        long remainingCooldown = cooldownManager.isActionAllowed(player, "abandon");
        if (remainingCooldown > 0) {
            long minutesLeft = TimeUnit.MILLISECONDS.toMinutes(remainingCooldown);
            Devotions.sendMessage(player, Messages.ABANDON_COOLDOWN_ACTIVE
                    .insertParsed("time", minutesLeft + " minutes")
            );
            return true;
        }

        // Check if the player has a devotion
        FavorManager favorManager = devotionManager.getPlayerDevotion(playerUniqueId);
        if (favorManager == null) {
            Devotions.sendMessage(player, Messages.NO_DEVOTION_SET);
            return true;
        }

        Deity deity = favorManager.getDeity();

        // Handle null deity
        if (deity == null) {
            return true;
        }

        // Check deity-specific abandonment conditions (if configured)
        if (deity.getAbandonCondition() != null) {
            String condition = PlaceholderAPI.setPlaceholders(player, deity.getAbandonCondition());
            boolean conditionMet = JavaScriptEngine.evaluateExpression(condition);

            if (!conditionMet) {
                Devotions.sendMessage(player, Messages.ABANDON_CONDITION_NOT_MET
                        .insertParsed("deity", deity.getName()));
                return true;
            }
        }

        // Penalize favor on abandonment
        int repeatFavor = plugin.getConfig().getInt("reselect-favor", 0);
        favorManager.setFavor(repeatFavor);

        // Mark the deity as abandoned
        devotionManager.markDeityAsAbandoned(playerUniqueId, deity.getName());

        // Set the abandon cooldown
        long abandonCooldown = plugin.getCooldownManager().getCooldownFromConfig("abandon-cooldown", "20m");
        cooldownManager.setCooldown(player, "abandon", abandonCooldown);

        // Remove the player's devotion
        devotionManager.removeDevotion(playerUniqueId);

        // Send abandonment message
        Devotions.sendMessage(player, Messages.DEVOTION_ABANDONED
                .insertParsed("deity", deity.getName())
        );

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
