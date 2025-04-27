package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import de.cubbossa.tinytranslations.libs.kyori.adventure.text.minimessage.tag.Tag;
import java.util.ArrayList;
import java.util.List;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.util.FavorUtils;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FavorCommand implements CommandExecutor, TabCompleter {

    private final Devotions plugin;

    public FavorCommand(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
        // Display player's favor if they don't provide an argument
        Player player = (Player) sender;
        if (args.length == 0) {

            // Check for permission
            if (!player.hasPermission("devotions.favor")) {
                Devotions.sendMessage(player, GlobalMessages.NO_PERM_CMD);
                return true;
            }

            // Get player's devotion
            FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(player.getUniqueId());
            if (favorManager == null) {
                Devotions.sendMessage(player, Messages.NO_DEVOTION_SET);
                return true;
            }

            String deityName = favorManager.getDeity().getName();
            Devotions.sendMessage(player, Messages.FAVOR_CURRENT
                    .insertString("deity", deityName)
                    .insertString("favor", String.valueOf(favorManager.getFavor()))
                    .insertTag("favor_col",
                            Tag.styling(s -> s.color(FavorUtils.getColorForFavor(favorManager.getFavor()))))
            );
            return true;
        } else if (args.length != 3) {
            Devotions.sendMessage(player, Messages.FAVOR_CMD_USAGE);
            return true;
        }

        // Check for permission
        else if (!player.hasPermission("devotions.admin")) {
            Devotions.sendMessage(player, GlobalMessages.NO_PERM_CMD);
            return true;
        }

        String action = args[0].toLowerCase();
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            Devotions.sendMessage(player, Messages.GENERAL_PLAYER_NOT_FOUND
                    .insertString("player", args[1]));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            Devotions.sendMessage(player, Messages.FAVOR_CMD_NUMBER_FORMAT);
            return true;
        }

        FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(targetPlayer.getUniqueId());
        if (favorManager == null) {
            Devotions.sendMessage(player, Messages.FAVOR_CMD_PLAYER_DOESNT_WORSHIP
                    .insertObject("player", targetPlayer));
            return true;
        }

        switch (action) {
            case "set" -> favorManager.setFavor(amount);
            case "give" -> favorManager.adjustFavor(amount);
            case "take" -> favorManager.adjustFavor(-amount);
            default -> {
                Devotions.sendMessage(player, Messages.FAVOR_CMD_INVALID_ACTION);
                return true;
            }
        }

        Devotions.sendMessage(player, Messages.FAVOR_SET_TO
                .insertString("deity", favorManager.getDeity().getName())
                .insertString("favor", String.valueOf(favorManager.getFavor()))
                .insertTag("favor_col", Tag.styling(s -> s.color(FavorUtils.getColorForFavor(favorManager.getFavor()))))
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
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
