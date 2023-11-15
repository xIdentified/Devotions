package me.xidentified.devotions.listeners;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.MeditationManager;
import me.xidentified.devotions.managers.RitualManager;
import me.xidentified.devotions.managers.ShrineManager;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.rituals.RitualObjective;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RitualListener implements Listener {
    private final Devotions plugin;
    private final RitualManager ritualManager;
    private final ShrineManager shrineManager;
    private static RitualListener instance;

    public RitualListener(Devotions plugin, ShrineManager shrineManager) {
        this.plugin = plugin;
        this.ritualManager = RitualManager.getInstance(plugin);
        this.shrineManager = shrineManager;
    }

    public static void initialize(Devotions plugin, ShrineManager shrineManager) {
        if (instance == null) {
            instance = new RitualListener(plugin, shrineManager);
        }
    }

    public static RitualListener getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RitualObjectiveListener has not been initialized. Call initialize() first.");
        }
        return instance;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Ritual ritual = ritualManager.getCurrentRitualForPlayer(player);

        // Return if player is not in a ritual
        if (ritual == null || ritual.isCompleted()) {
            return;
        }

        // Check if player moved, if in meditation. Reset timer if they moved.
        if (meditationManager().hasPlayerMovedSince(player) && meditationManager().isPlayerInMeditation(player)) {
            plugin.debugLog("Player " + player.getName() + " moved during meditation.");
            player.sendMessage(MessageUtils.parse("<red>You moved during meditation! Restarting timer..."));
            meditationManager().startMeditation(player, ritual, getMeditationObjective(ritual));
        }

        // Check and update gathering objectives
        for (RitualObjective objective : ritual.getObjectives()) {
            if (objective.getType() == RitualObjective.Type.GATHERING && !objective.isComplete()) {
                int itemCount = countItemsInInventory(player.getInventory(), Material.valueOf(objective.getTarget()));
                if (itemCount >= objective.getCount()) {
                    objective.setCurrentCount(itemCount);
                }
            }
        }

        // Check if player has returned to shrine with all objectives completed regardless of the objective type
        if (isPlayerNearShrine(player) && allObjectivesCompleted(ritual)) {
            plugin.debugLog("Player " + player.getName() + " returned to the shrine with all objectives completed.");
            plugin.getRitualManager().completeRitual(player, ritual, meditationManager());
        }

    }

    // Count items in inventory for 'gathering' objective
    private int countItemsInInventory(Inventory inventory, Material material) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private RitualObjective getMeditationObjective(Ritual ritual) {
        for (RitualObjective objective : ritual.getObjectives()) {
            if (objective.getType() == RitualObjective.Type.MEDITATION) {
                return objective;
            }
        }
        return null;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            Ritual ritual = ritualManager.getCurrentRitualForPlayer(player);

            if (ritual == null || ritual.isCompleted()) {
                return;
            }

            List<RitualObjective> objectives = ritual.getObjectives();
            for (RitualObjective objective : objectives) {
                if (objective.getType() == RitualObjective.Type.PURIFICATION && objective.getTarget().equals(event.getEntityType().name())) {
                    objective.incrementCount();
                    if (objective.isComplete()) {
                        plugin.debugLog("Objective complete for ritual " + ritual.getDisplayName());
                        player.sendMessage(MessageUtils.parse("<light_purple>Return to the shrine to complete the ritual."));
                    }
                }
            }
        }
    }

    // Check if player has returned to shrine to complete the ritual
    private boolean isPlayerNearShrine(Player player) {
        // Check if the player is within the shrine's radius
        Location shrineLocation = shrineManager.getShrineLocationForPlayer(player);
        if (shrineLocation != null && player.getWorld().equals(shrineLocation.getWorld())) {
            double distance = player.getLocation().distance(shrineLocation);
            return distance <= 5;
        }
        return false;
    }

    private boolean allObjectivesCompleted(Ritual ritual) {
        return ritual.getObjectives().stream().allMatch(RitualObjective::isComplete);
    }

    private MeditationManager meditationManager(){
        return plugin.getMeditationManager();
    }
}
