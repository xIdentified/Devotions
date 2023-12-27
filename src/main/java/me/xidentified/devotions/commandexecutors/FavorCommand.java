package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.util.MessageUtils;
import me.xidentified.devotions.util.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FavorCommand implements CommandExecutor, TabCompleter {

    private final Devotions plugin;

    public FavorCommand(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        // Display player's favor if they don't provide an argument
        Player player = (Player) sender;

        if (args.length == 0) {
            FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(player.getUniqueId());
            if (favorManager == null || favorManager.getDeity() == null) {
                plugin.sendMessage(player, Messages.FAVOR_NO_DEVOTION_SET);
            } else {
                String favorText = MessageUtils.getFavorText(favorManager.getFavor());
                String message = "Your current favor with " + favorManager.getDeity().getName() + " is " + favorText;
                // TODO: Language support
                MessageUtils.sendMessage(player, message);
            }
            return true;
        }

        if (args.length != 3) {
            plugin.sendMessage(player,Messages.FAVOR_CMD_USAGE);
            return true;
        }

        // Check for permission
        if (!player.hasPermission("devotions.admin")) {
            plugin.sendMessage(player, Messages.GENERAL_CMD_NO_PERM);
            return true;
        }

        String action = args[0].toLowerCase();
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            plugin.sendMessage(player, Messages.GENERAL_PLAYER_NOT_FOUND.formatted(
                Placeholder.unparsed("player", args[1])
            ));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            plugin.sendMessage(player,Messages.FAVOR_CMD_NUMBER_FORMAT);
            return true;
        }

        FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(targetPlayer.getUniqueId());
        if (favorManager == null) {
            plugin.sendMessage(player,Messages.FAVOR_CMD_PLAYER_DOESNT_WORSHIP.formatted(
                Placeholder.unparsed("player", targetPlayer.getName())
            ));
            return true;
        }

        switch (action) {
            case "set" -> favorManager.setFavor(amount);
            case "give" -> favorManager.increaseFavor(amount);
            case "take" -> favorManager.decreaseFavor(amount);
            default -> {
                plugin.sendMessage(player,Messages.FAVOR_CMD_INVALID_ACTION);
                return true;
            }
        }

        String favorText = MessageUtils.getFavorText(favorManager.getFavor());
        plugin.sendMessage(player, Messages.FAVOR_SET_TO.formatted(
                Placeholder.unparsed("player", targetPlayer.getName()),
                Placeholder.unparsed("favor", favorText)
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("set");
            completions.add("give");
            completions.add("take");
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }

        return completions;
    }
}