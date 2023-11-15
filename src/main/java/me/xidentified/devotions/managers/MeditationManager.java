package me.xidentified.devotions.managers;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.rituals.RitualObjective;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class MeditationManager {

    private final Devotions plugin;
    private final Map<Player, MeditationData> meditationStartData = new HashMap<>();
    private final Map<Player, BukkitRunnable> meditationTimers = new HashMap<>();

    public MeditationManager(Devotions plugin) {
        this.plugin = plugin;
    }

    public void startMeditation(Player player, Ritual ritual, RitualObjective objective) {
        meditationStartData.put(player, new MeditationData(System.currentTimeMillis(), player.getLocation()));

        // Cancel any existing timer for the player
        BukkitRunnable existingTimer = meditationTimers.get(player);
        if (existingTimer != null) {
            existingTimer.cancel();
        }

        BukkitRunnable newTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!hasPlayerMovedSince(player)) {
                    objective.setCurrentCount(objective.getCount());

                    if (objective.isComplete()) {
                        plugin.debugLog("[DEBUG] Meditation objective complete for player: " + player.getName());
                        player.sendMessage(MessageUtils.parse("<green>Meditation complete! You can now move."));
                        plugin.getRitualManager().completeRitual(player, ritual, plugin.getMeditationManager());
                    }
                } else {
                    plugin.debugLog("[DEBUG] Player moved. Restarting meditation for player: " + player.getName());
                    player.sendMessage(MessageUtils.parse("<red>You moved during meditation! Restarting timer..."));
                    startMeditation(player, ritual, objective); // Restart meditation timer
                    meditationTimers.remove(player);
                }
            }
        };

        meditationTimers.put(player, newTimer);
        newTimer.runTaskLater(plugin, 20L * objective.getCount());  // 20 ticks/second * config value
    }

    public boolean hasPlayerMovedSince(Player player) {
        MeditationData meditationData = meditationStartData.get(player);
        if (meditationData == null) {
            return true; // If there's no recorded start data, assume the player has moved
        }
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

record MeditationData(long startTime, Location initialLocation) {
}
