package me.xidentified.devotions.managers;

import java.util.HashMap;
import java.util.Map;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.rituals.MeditationData;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.rituals.RitualObjective;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MeditationManager {

    private final Devotions plugin;
    private final Map<Player, MeditationData> meditationStartData = new HashMap<>();
    private final Map<Player, BukkitRunnable> meditationTimers = new HashMap<>();

    public MeditationManager(Devotions plugin) {
        this.plugin = plugin;
    }

    public MeditationData getMeditationData(Player player) {
        return meditationStartData.get(player);
    }

    public void startMeditation(Player player, Ritual ritual, RitualObjective objective) {
        meditationStartData.put(player, new MeditationData(System.currentTimeMillis(), player.getLocation()));

        // Cancel any existing timer for the player
        cancelMeditationTimer(player);

        BukkitRunnable newTimer = new BukkitRunnable() {
            @Override
            public void run() {
                MeditationData meditationData = meditationStartData.get(player);
                if (meditationData != null && !hasPlayerMovedSince(player, meditationData)) {
                    completeMeditationObjective(player, ritual, objective);
                } else {
                    applyMeditationPenalties(player, meditationData);
                }
            }
        };

        meditationTimers.put(player, newTimer);
        newTimer.runTaskLater(plugin, 20L * objective.getCount());
    }

    private void completeMeditationObjective(Player player, Ritual ritual, RitualObjective objective) {
        objective.setCurrentCount(objective.getCount());
        if (objective.isComplete()) {
            plugin.debugLog("[DEBUG] Meditation objective complete for player: " + player.getName());
            Devotions.sendMessage(player, Messages.MEDITATION_COMPLETE);
            plugin.getRitualManager().completeRitual(player, ritual, this);
        }
        clearMeditationData(player);
    }

    public void applyMeditationPenalties(Player player, MeditationData meditationData) {
        if (meditationData != null) {
            int moveCount = meditationData.moveCounter().incrementAndGet();

            // Smite the player as a warning
            if (moveCount == 1) {
                player.getWorld().strikeLightningEffect(player.getLocation());
                Devotions.sendMessage(player, Messages.MEDITATION_PENALTY);
            }

            // Cancel the ritual if they keep moving
            else if (moveCount > 2) {
                String ritualName = plugin.getRitualManager().getCurrentRitualForPlayer(player).getDisplayName();
                cancelMeditationTimer(player);
                clearMeditationData(player);
                Devotions.sendMessage(player, Messages.RITUAL_CANCELLED.insertParsed("ritual", ritualName));
                plugin.getRitualManager().cancelRitualFor(player);
            }
        }
    }

    public boolean hasPlayerMovedSince(Player player, MeditationData meditationData) {
        Location initialLocation = meditationData.initialLocation();
        return !locationsAreEqual(initialLocation, player.getLocation());
    }

    private boolean locationsAreEqual(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }

    public void cancelMeditationTimer(Player player) {
        BukkitRunnable existingTimer = meditationTimers.get(player);
        if (existingTimer != null) {
            existingTimer.cancel();
            meditationTimers.remove(player);
        }
    }

    public void clearMeditationData(Player player) {
        meditationStartData.remove(player);
    }

    public boolean isPlayerInMeditation(Player player) {
        return meditationStartData.containsKey(player);
    }

}
