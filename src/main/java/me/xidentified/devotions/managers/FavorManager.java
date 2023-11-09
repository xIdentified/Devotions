package me.xidentified.devotions.managers;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.storage.DevotionStorage;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

// Point system for tracking favor with each deity
public class FavorManager {
    // Basic details
    private final Devotions plugin;
    private final UUID uuid;
    private Deity deity;
    private int favor;
    private final int maxFavor;

    // Blessing, curse, miracle thresholds and chances etc
    private final int BLESSING_THRESHOLD;
    private final int CURSE_THRESHOLD;
    private final double BLESSING_CHANCE;
    private final double CURSE_CHANCE;
    private final int MIRACLE_THRESHOLD;
    private final double MIRACLE_CHANCE;
    private final long MIRACLE_DURATION;
    private long lastTimeBelowMiracleThreshold;

    // Decay system variables
    private long lastDecayTime; // Tracks the last time the favor was decayed
    private final int decayRate; // The amount of favor that decays per interval
    private final long decayInterval; // How often (in ticks) the decay check runs

    public FavorManager(Devotions plugin, UUID playerUUID, Deity deity) {
        // Basic details
        this.plugin = plugin;
        this.uuid = playerUUID;
        this.deity = deity;
        this.favor = plugin.getConfig().getInt("initial-favor");
        this.maxFavor = plugin.getConfig().getInt("max-favor", 250);

        // Blessing, curse, miracle thresholds and chances etc
        this.BLESSING_THRESHOLD = plugin.getConfig().getInt("blessingThreshold");
        this.CURSE_THRESHOLD = plugin.getConfig().getInt("curseThreshold");
        this.BLESSING_CHANCE = plugin.getConfig().getDouble("blessingChance");
        this.CURSE_CHANCE = plugin.getConfig().getDouble("curseChance");
        this.MIRACLE_THRESHOLD = plugin.getConfig().getInt("miracleThreshold", 90);
        this.MIRACLE_CHANCE = plugin.getConfig().getDouble("miracleChance");
        this.MIRACLE_DURATION = plugin.getConfig().getInt("miracleDuration", 3) * 24000L; // 3 in-game days, 24000 ticks per day

        // Decay system variables
        this.decayRate = plugin.getConfig().getInt("decay-rate", 5);
        this.decayInterval = plugin.getConfig().getLong("decay-interval", 1200) * 20L; // Default 1200 seconds (20 minutes) converted to ticks
        this.lastDecayTime = System.currentTimeMillis();

        // Check for effects (Blessings, Curses, Miracles)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkForEffects, 0L, 20L * 5);  // Check every 10 minutes

        // Check for favor decay (if player hasn't worshipped deity in too long)
        Bukkit.getScheduler().runTaskTimer(plugin, this::decayFavor, 0L, 20L); // Check every second
    }

    private void checkForEffects() {
        Player player = Bukkit.getPlayer(uuid); // Get the player from the UUID

        if (player != null && player.isOnline()) { // Check if the player is online
            long currentTime = System.currentTimeMillis();

            // Check for blessings
            if (favor >= BLESSING_THRESHOLD && Math.random() < BLESSING_CHANCE) {
                deity.applyBlessing(player, deity);
            }

            // Check for curses
            if (favor <= CURSE_THRESHOLD && Math.random() < CURSE_CHANCE) {
                deity.applyCurse(player, deity);
            }

            // Check for miracles
            if (favor >= MIRACLE_THRESHOLD) {
                if (lastTimeBelowMiracleThreshold == 0 || (currentTime - lastTimeBelowMiracleThreshold) >= MIRACLE_DURATION) {
                    if (Math.random() < MIRACLE_CHANCE) {
                        deity.applyMiracle(player);
                        lastTimeBelowMiracleThreshold = 0; // Reset timer
                    }
                }
            } else {
                lastTimeBelowMiracleThreshold = currentTime; // Update timer if favor drops below the threshold
            }
        }
        // If player is offline, don't apply effects
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public Deity getDeity() {
        return deity;
    }

    public int getFavor() {
        return favor;
    }

    public void setFavor(int favor) {
        this.favor = favor;
    }

    public void setDeity(Deity newDeity) { this.deity = newDeity; }

    public void increaseFavor(int amount) {
        this.favor += amount;
        Player player = getPlayer();

        if (this.favor > maxFavor) {
            this.favor = maxFavor;
        }
        if (player != null && deity != null) {
            player.sendMessage(MessageUtils.parse("<green>Your favor with " + deity.getName() + " has increased to " + this.favor + "."));

            DevotionStorage devotionStorage = plugin.getDevotionStorage();
            devotionStorage.savePlayerDevotion(player.getUniqueId(), this);
        } else {
            System.err.println("Error: Player or Deity is null in PlayerDevotion::increaseFavor");
        }
    }

    public void decreaseFavor(int amount) {
        this.favor -= amount;
        if (this.favor < 0) {
            this.favor = 0;
        }

        Player player = getPlayer();
        if (player != null && player.isOnline() && deity != null) {
            player.sendMessage(MessageUtils.parse("<red>Your favor with " + deity.getName() + " has decreased to " + this.favor + "."));
        }

        DevotionStorage devotionStorage = plugin.getDevotionStorage();
        devotionStorage.savePlayerDevotion(uuid, this);
    }

    private void decayFavor() {
        long currentTime = System.currentTimeMillis();

        // Calculate the time since the last decay in ticks
        long timeSinceLastDecay = (currentTime - lastDecayTime) / 50; // Convert milliseconds to ticks (1 tick = 50ms)
        if (timeSinceLastDecay >= decayInterval) {
            // Update the last decay time
            lastDecayTime = currentTime;

            // Decay the favor without notifying the player
            decreaseFavor(decayRate);
        }
    }

}