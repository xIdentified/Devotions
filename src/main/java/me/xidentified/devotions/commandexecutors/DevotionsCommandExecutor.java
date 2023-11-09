package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DevotionsCommandExecutor implements CommandExecutor {

    private final Devotions plugin;

    public DevotionsCommandExecutor(Devotions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("devotions.reload")) {
                sendMessage((Player) sender, "You do not have permission to use this command.");
                return true;
            }

            plugin.reloadConfigurations();
            sendMessage((Player) sender,"<green>Devotions successfully reloaded!");
            return true;
        }

        return false;
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(MessageUtils.parse(message));
    }

}
