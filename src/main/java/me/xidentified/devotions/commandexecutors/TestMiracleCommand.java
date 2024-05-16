package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Miracle;
import me.xidentified.devotions.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestMiracleCommand implements CommandExecutor, TabCompleter {
    private final List<Miracle> miraclesList;
    private final Map<String, Miracle> miraclesMap;

    public TestMiracleCommand(Map<String, Miracle> miracles) {
        this.miraclesMap = miracles;
        this.miraclesList = new ArrayList<>(miracles.values());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("devotions.admin")) {
            return false;
        }

        if (miraclesList.isEmpty()) {
            Devotions.sendMessage(sender, Messages.MIRACLE_CMD_NO_MIRACLES);
            return true;
        }

        if (args.length != 1) {
            Devotions.sendMessage(player, Messages.MIRACLE_CMD_USAGE);
            return true;
        }

        try {
            int index = Integer.parseInt(args[0]) - 1; // Convert to zero-based index
            if (index < 0 || index >= miraclesList.size()) {
                throw new IndexOutOfBoundsException();
            }

            Miracle miracle = miraclesList.get(index);
            miracle.apply(player);

        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            Devotions.sendMessage(player, Messages.MIRACLE_CMD_UNKNOWN_MIRACLE);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("devotions.admin")) {
            return Collections.emptyList();
        }

        // Generate a list of strings formatted as "number - miracle name"
        List<String> completions = new ArrayList<>();
        int index = 1;
        for (String key : miraclesMap.keySet()) {
            completions.add(index++ + " - " + key);
        }
        return completions;
    }
}
