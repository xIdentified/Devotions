package me.xidentified.devotions.commandexecutors;

import de.cubbossa.tinytranslations.GlobalMessages;
import de.cubbossa.tinytranslations.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.managers.ShrineManager;
import me.xidentified.devotions.util.Messages;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
        if (!(sender instanceof Player player)) {
            Devotions.sendMessage(sender, GlobalMessages.CMD_PLAYER_ONLY);
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("list")) {
                displayShrineList(player);
                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (player.hasPermission("devotions.shrine.remove")) {
                    pendingShrineRemovals.put(player.getUniqueId(), true);
                    Devotions.sendMessage(player, Messages.SHRINE_RC_TO_REMOVE);
                    //Bukkit.getLogger().log(Level.WARNING, "Current pendingShrineRemovals map: " + pendingShrineRemovals);
                } else {
                    Devotions.sendMessage(player, Messages.SHRINE_NO_PERM_REMOVE);
                }
                return true;
            }
        } else if (player.hasPermission("devotions.shrine.set")) {
            int currentShrineCount = shrineManager.getShrineCount(player);
            int shrineLimit = shrineManager.getPlugin().getDevotionsConfig().getShrineLimit();

            if (currentShrineCount >= shrineLimit) {
                Devotions.sendMessage(player, Messages.SHRINE_LIMIT_REACHED.insertNumber("limit", shrineLimit));
                return true;
            }

            // If the player doesn't follow a deity, don't let them make a shrine
            FavorManager favorManager = devotionManager.getPlayerDevotion(player.getUniqueId());

            if (favorManager == null || favorManager.getDeity() == null) {
                Devotions.sendMessage(player, Messages.SHRINE_FOLLOW_DEITY_TO_DESIGNATE);
                return true;
            }

            // Fetch the deity directly from the player's FavorManager
            Deity deity = favorManager.getDeity();
            pendingShrineDesignations.put(player, deity);
            Devotions.sendMessage(player,
                    Messages.SHRINE_CLICK_BLOCK_TO_DESIGNATE.insertParsed("deity", deity.getName()));
            return true;
        } else {
            Devotions.sendMessage(player, Messages.SHRINE_NO_PERM_SET);
            return false;
        }
        return false;
    }

    @EventHandler
    public void onShrineDesignation(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (pendingShrineDesignations.containsKey(player)) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                Shrine existingShrine = shrineManager.getShrineAtLocation(clickedBlock.getLocation());
                if (existingShrine != null) {
                    // Inform the player that a shrine already exists at this location
                    Devotions.sendMessage(player, Messages.SHRINE_ALREADY_EXISTS
                            .insertParsed("deity", existingShrine.getDeity().getName())
                            .insertString("location", clickedBlock.getLocation().toString())
                    );
                } else {
                    Deity deity = pendingShrineDesignations.remove(player);
                    // Store the clickedBlock location, deity, and player as a designated shrine
                    Shrine newShrine = new Shrine(clickedBlock.getLocation(), deity, player.getUniqueId());
                    shrineManager.addShrine(newShrine);
                    Devotions.sendMessage(player, Messages.SHRINE_SUCCESS.insertParsed("deity", deity.getName()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShrineRemoval(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if the player has initiated the shrine removal process
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (pendingShrineRemovals.containsKey(playerId)) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                Location location = clickedBlock.getLocation();
                Shrine shrine = shrineManager.getShrineAtLocation(location);
                if (shrine != null) {
                    boolean canRemove = player.hasPermission("devotions.admin") || shrine.getOwner() == playerId;
                    if (canRemove) {
                        boolean removed = shrineManager.removeShrine(playerId, location);
                        if (removed) {
                            Devotions.sendMessage(player, Messages.SHRINE_REMOVED);
                        } else {
                            Devotions.sendMessage(player, Messages.SHRINE_REMOVE_FAIL);
                        }
                    } else {
                        Devotions.sendMessage(player, Messages.SHRINE_REMOVE_FAIL);
                    }
                } else {
                    Devotions.sendMessage(player, Messages.SHRINE_REMOVE_NOT_FOUND);
                }
                pendingShrineRemovals.remove(playerId);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length > 0 && sender.hasPermission("devotions.shrine.list")) {
            completions.add("list");
            completions.add("remove");
        } else if (args.length > 0) {
            completions.add("remove");
        }

        return completions;
    }

    private void displayShrineList(Player player) {
        List<Shrine> shrines;
        boolean isAdmin = player.hasPermission("devotions.admin");

        if (isAdmin) {
            shrines = shrineManager.getAllShrines();
        } else {
            shrines = shrineManager.getShrinesByOwner(player.getUniqueId());
        }

        if (shrines.isEmpty()) {
            Devotions.sendMessage(player, Messages.SHRINE_NO_DESIGNATED_SHRINE);
            return;
        }

        Devotions.sendMessage(player, Messages.SHRINE_LIST);
        for (Shrine shrine : shrines) {
            Location loc = shrine.getLocation();
            Message message = isAdmin ? Messages.SHRINE_INFO_ADMIN : Messages.SHRINE_INFO_PLAYER;
            Devotions.sendMessage(player, message
                    .insertParsed("deity", shrine.getDeity().getName())
                    .insertNumber("x", loc.getBlockX())
                    .insertNumber("y", loc.getBlockY())
                    .insertNumber("z", loc.getBlockZ())
            );
        }
    }

}
