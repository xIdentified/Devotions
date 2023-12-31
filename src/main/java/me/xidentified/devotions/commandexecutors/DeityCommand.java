package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.util.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
            Devotions.getInstance().sendMessage(sender, GlobalMessages.CMD_PLAYER_ONLY);
            return true;
        }

        if (args.length < 1) {
            plugin.sendMessage(player, Messages.DEITY_CMD_USAGE);
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
                plugin.sendMessage(player,Messages.DEITY_CMD_USAGE);
                return true;
            }
        }
    }

    private boolean handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(player, Messages.DEITY_CMD_SPECIFY_DEITY);
            return true;
        }

        String deityName = args[1];
        Deity selectedDeity = plugin.getDevotionManager().getDeityByName(deityName);

        if (selectedDeity == null) {
            plugin.sendMessage(player, Messages.DEITY_NOT_FOUND);
            return false;
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
            plugin.sendMessage(player,Messages.DEITY_SPECIFY_PLAYER);
            return true;
        }

        String deityName = args[1];
        Deity selectedDeity = plugin.getDevotionManager().getDeityByName(deityName);

        if (selectedDeity == null) {
            plugin.sendMessage(player,Messages.DEITY_NOT_FOUND);
            return false;
        }

        // Display deity information
        plugin.sendMessage(player, Messages.DEITY_INFO.formatted(
            Placeholder.unparsed("name", selectedDeity.getName()),
            Placeholder.unparsed("lore", selectedDeity.getLore()),
            Placeholder.unparsed("domain", String.join(", ", selectedDeity.getDomain())),
            Placeholder.unparsed("alignment", selectedDeity.getAlignment()),
            Placeholder.unparsed("rituals", selectedDeity.getRituals()),
            Placeholder.unparsed("offerings", selectedDeity.getOfferings())
        ));
        return true;
    }

    private boolean handleList(Player player) {
        List<Deity> deities = plugin.getDevotionManager().getAllDeities();
        if (deities.isEmpty()) {
            plugin.sendMessage(player,Messages.DEITY_NO_DEITY_FOUND);
            return false;
        }

        plugin.sendMessage(player, Messages.DEITY_LIST_HEADER);
        for (Deity deity : deities) {
            plugin.sendMessage(player, Messages.DEITY_LIST_ENTRY.formatted(
                Placeholder.unparsed("name", deity.name)
            ));
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

}
