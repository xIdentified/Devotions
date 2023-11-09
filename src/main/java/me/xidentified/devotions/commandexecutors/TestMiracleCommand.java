package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Miracle;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TestMiracleCommand implements CommandExecutor {
    private final Map<String, Miracle> miracles;

    public TestMiracleCommand(Map<String, Miracle> miracles) {
        this.miracles = miracles;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = (Player) sender;

        if (player.hasPermission("devotions.admin")) {
            if (miracles.isEmpty()) {
                sendMessage((Player) sender, "<red>No miracles are loaded.");
                return true;
            }

            sendMessage((Player) sender, "<green>Available miracles: <yellow>" + miracles.keySet());

            if (args.length != 1) {
                sendMessage(player, "<yellow>Usage: /testmiracle <miracleName>");
                return true;
            }

            String miracleName = args[0];
            Miracle miracle = miracles.get(miracleName);

            if (miracle == null) {
                sendMessage(player, "<red>Unknown miracle: " + miracleName);
                return true;
            }

            miracle.apply(player);
            sendMessage(player, "<green>Applied miracle: <yellow>" + miracleName);

            return true;
        }
        return false;
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(MessageUtils.parse(message));
    }

}
