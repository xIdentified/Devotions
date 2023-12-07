package me.xidentified.devotions.commandexecutors;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.managers.ShrineManager;
import me.xidentified.devotions.util.MessageUtils;
import me.xidentified.devotions.util.Messages;
import me.xidentified.devotions.util.Placeholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
            Devotions.getInstance().sendMessage(sender, Messages.GENERAL_CMD_PLAYER_ONLY);
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("list")) {
                if (player.hasPermission("devotions.shrine.list")) {
                    displayShrineList(player);
                } else {
                    Devotions.getInstance().sendMessage(player, Messages.SHRINE_NO_PERM_LIST);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (player.hasPermission("devotions.shrine.remove")) {
                    pendingShrineRemovals.put(player.getUniqueId(), true);
                    Devotions.getInstance().sendMessage(player, Messages.SHRINE_RC_TO_REMOVE);
                    //Bukkit.getLogger().log(Level.WARNING, "Current pendingShrineRemovals map: " + pendingShrineRemovals);
                } else {
                    Devotions.getInstance().sendMessage(player, Messages.SHRINE_NO_PERM_REMOVE);
                }
                return true;
            }
        } else if (player.hasPermission("devotions.shrine.set")) {
            int currentShrineCount = shrineManager.getShrineCount(player);
            int shrineLimit = shrineManager.getPlugin().getShrineLimit();

            if (currentShrineCount >= shrineLimit) {
                Devotions.getInstance().sendMessage(player, Messages.SHRINE_LIMIT_REACHED.formatted(
                    Formatter.number("limit", shrineLimit)
                ));
                return true;
            }

            // If the player doesn't follow a deity, don't let them make a shrine
            FavorManager favorManager = devotionManager.getPlayerDevotion(player.getUniqueId());
            if (favorManager == null || favorManager.getDeity() == null) {
                Devotions.getInstance().sendMessage(player, Messages.SHRINE_FOLLOW_DEITY_TO_DESIGNATE);
                return true;
            }

            // Fetch the deity directly from the player's FavorManager
            Deity deity = favorManager.getDeity();
            if (deity != null) {
                pendingShrineDesignations.put(player, deity);
                Devotions.getInstance().sendMessage(player, Messages.SHRINE_CLICK_BLOCK_TO_DESIGNATE.formatted(
                    Placeholder.unparsed("deity", deity.getName())
                ));
            } else {
                Devotions.getInstance().sendMessage(player, Messages.SHRINE_DEITY_NOT_FOUND);
            }
            return true;
        } else {
            Devotions.getInstance().sendMessage(player, Messages.SHRINE_NO_PERM_SET);
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
                Devotions.getInstance().sendMessage(player,Messages.SHRINE_SUCCESS.formatted(
                    Placeholder.unparsed("deity", deity.getName())
                ));
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
                        Devotions.getInstance().sendMessage(player, Messages.SHRINE_REMOVED);
                    } else {
                        Devotions.getInstance().sendMessage(player, Messages.SHRINE_REMOVE_FAIL);
                    }
                } else {
                    Devotions.getInstance().sendMessage(player, Messages.SHRINE_REMOVE_NOT_FOUND);
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
            Devotions.getInstance().sendMessage(player, Messages.SHRINE_NO_DESIGNATED_SHRINE);
            return;
        }

        Devotions.getInstance().sendMessage(player, Messages.SHRINE_LIST);
        for (Shrine shrine : shrines) {
            Location loc = shrine.getLocation();
            Devotions.getInstance().sendMessage(player, Messages.SHRINE_INFO.formatted(
                Placeholder.unparsed("deity", shrine.getDeity().getName()),
                Formatter.number("x", loc.getBlockX()),
                Formatter.number("y", loc.getBlockZ()),
                Formatter.number("z", loc.getBlockY())
            ));
        }
    }
}
