package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.managers.ShrineManager;
import me.xidentified.devotions.util.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;

public class ShrineCommandExecutor implements CommandExecutor, Listener, TabCompleter {
    private final Map<Player, Deity> pendingShrineDesignations = new HashMap<>();
    private final Map<UUID, Boolean> pendingShrineRemovals = new HashMap<>();
    private final DevotionManager devotionManager;
    private final ShrineManager shrineManager;

    public ShrineCommandExecutor(DevotionManager devotionManager, ShrineManager shrineManager) {
        this.devotionManager = devotionManager;
        this.shrineManager = shrineManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.parse("<red>Only players can use this command."));
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("list")) {
                if (player.hasPermission("devotions.shrine.list")) {
                    displayShrineList(player);
                } else {
                    player.sendMessage(MessageUtils.parse("<red>You don't have permission to list shrines."));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (player.hasPermission("devotions.shrine.remove")) {
                    pendingShrineRemovals.put(player.getUniqueId(), true);
                    player.sendMessage(MessageUtils.parse("<yellow>Right-click on a shrine to remove it."));
                    Bukkit.getLogger().log(Level.WARNING, "Current pendingShrineRemovals map: " + pendingShrineRemovals);
                } else {
                    player.sendMessage(MessageUtils.parse("<red>You don't have permission to remove shrines."));
                }
                return true;
            }
        } else if (player.hasPermission("devotions.shrine.set")) {
            int currentShrineCount = shrineManager.getShrineCount(player);
            int shrineLimit = shrineManager.getPlugin().getShrineLimit();

            if (currentShrineCount >= shrineLimit) {
                sendMessage(player, "<red>You have reached the maximum number of shrines (" + shrineLimit + ").");
                return true;
            }

            // If the player doesn't follow a deity, don't let them make a shrine
            FavorManager favorManager = devotionManager.getPlayerDevotion(player.getUniqueId());
            if (favorManager == null || favorManager.getDeity() == null) {
                sendMessage(player, "<red>You need to follow a deity to designate an altar!");
                return true;
            }

            // Fetch the deity directly from the player's FavorManager
            Deity deity = favorManager.getDeity();
            if (deity != null) {
                pendingShrineDesignations.put(player, deity);
                sendMessage(player, "<yellow>Right-click on a block to designate it as a shrine for " + deity.getName());
            } else {
                sendMessage(player, "<red>Error: Could not determine your deity.");
            }
            return true;
        } else {
            player.sendMessage(MessageUtils.parse("<red>You don't have permission to set a shrine."));
            return false;
        }
        return false;
    }

    @EventHandler
    public void onShrineDesignation(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (pendingShrineDesignations.containsKey(player)) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && shrineManager.getShrineAtLocation(clickedBlock.getLocation()) == null) {
                Deity deity = pendingShrineDesignations.remove(player);
                // Store the clickedBlock location, deity, and player as a designated shrine
                Shrine newShrine = new Shrine(clickedBlock.getLocation(), deity, player.getUniqueId());
                shrineManager.addShrine(newShrine);
                sendMessage(player,"<green>Successfully designated a shrine for " + deity.getName() + "!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShrineRemoval(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (pendingShrineRemovals.containsKey(playerId)) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                Location location = clickedBlock.getLocation();
                if (shrineManager.getShrineAtLocation(location) != null) {
                    boolean removed = shrineManager.removeShrine(playerId, location);
                    if (removed) {
                        player.sendMessage(MessageUtils.parse("<green>Shrine removed successfully!"));
                    } else {
                        player.sendMessage(MessageUtils.parse("<red>Failed to remove shrine. You might not own it."));
                    }
                } else {
                    player.sendMessage(MessageUtils.parse("<red>No shrine found at this location."));
                }
                pendingShrineRemovals.remove(playerId);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length >0 && sender.hasPermission("devotions.shrine.list")) {
            completions.add("list");
            completions.add("remove");
        }
        else if (args.length >0) {
            completions.add("remove");
        }

        return completions;
    }

    private void displayShrineList(Player player) {
        List<Shrine> shrines = shrineManager.getAllShrines();
        if (shrines.isEmpty()) {
            player.sendMessage(MessageUtils.parse("<red>There are no designated shrines."));
            return;
        }

        player.sendMessage(MessageUtils.parse("<yellow>Shrines:"));
        for (Shrine shrine : shrines) {
            Location loc = shrine.getLocation();
            TextComponent message = Component.text()
                    .content(shrine.getDeity().getName() + " at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                    .clickEvent(ClickEvent.runCommand("/tp " + player.getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()))
                    .build();
            player.sendMessage(message);
        }
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(MessageUtils.parse(message));
    }

}
