package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
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

public class DevotionsCommandExecutor implements CommandExecutor, TabCompleter {

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
                plugin.sendMessage(sender, Messages.GENERAL_CMD_NO_PERM);
                return true;
            }

            plugin.reloadConfigurations();
            plugin.sendMessage(sender,Messages.DEVOTION_RELOAD_SUCCESS);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
        }

        return completions;
    }
}
