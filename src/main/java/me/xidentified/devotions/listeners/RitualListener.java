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
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

        if (ritual == null || ritual.isCompleted()) {
            return;
        }

        // Check if player moved, if in meditation
        if (meditationManager().hasPlayerMovedSince(player) && meditationManager().isPlayerInMeditation(player)) {
            plugin.debugLog("Player " + player.getName() + " moved during meditation.");
            player.sendMessage(MessageUtils.parse("<red>You moved during meditation! Restarting timer..."));
            meditationManager().startMeditation(player, ritual, getMeditationObjective(ritual));
        }

        AtomicBoolean allObjectivesMet = new AtomicBoolean(true);

        List<RitualObjective> objectives = ritual.getObjectives();
        for (RitualObjective objective : objectives) {
            if (objective == null) {
                continue;
            }

            if (!objective.isComplete()) {
                allObjectivesMet.set(false);

                switch (objective.getType()) {
                    case GATHERING -> {
                        ItemStack requiredItem = new ItemStack(Material.valueOf(objective.getTarget()), objective.getCount());
                        if (player.getInventory().containsAtLeast(requiredItem, objective.getCount())) {
                            if (isPlayerNearShrine(player)) {
                                objective.incrementCount();
                            }
                        }
                    }
                    case PURIFICATION -> {
                        // Handled in the EntityDeathEvent listener
                    }
                    case MEDITATION -> {
                        // Meditation check done above
                    }
                }
            }
        }

        if (allObjectivesMet.get()) {
            plugin.getRitualManager().completeRitual(player, ritual, meditationManager());
        }
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
                    if (objective.isComplete() && isPlayerNearShrine(player)) {
                        plugin.getRitualManager().completeRitual(player, ritual, meditationManager());
                    }
                }
            }
        }
    }

    private boolean isPlayerNearShrine(Player player) {
        // Check if the player is within the shrine's radius
        Location shrineLocation = shrineManager.getShrineLocationForPlayer(player);
        if (shrineLocation != null && player.getWorld().equals(shrineLocation.getWorld())) {
            double distance = player.getLocation().distance(shrineLocation);
            return distance <= 3;
        }
        return false;
    }

    private MeditationManager meditationManager(){
        return plugin.getMeditationManager();
    }
}
