package me.xidentified.devotions.managers;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.storage.DevotionStorage;
import me.xidentified.devotions.util.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        this.BLESSING_THRESHOLD = plugin.getConfig().getInt("blessing-threshold");
        this.CURSE_THRESHOLD = plugin.getConfig().getInt("curse-threshold");
        this.BLESSING_CHANCE = plugin.getConfig().getDouble("blessing-chance");
        this.CURSE_CHANCE = plugin.getConfig().getDouble("curse-chance");
        this.MIRACLE_THRESHOLD = plugin.getConfig().getInt("miracle-threshold", 90);
        this.MIRACLE_CHANCE = plugin.getConfig().getDouble("miracle-chance");
        this.MIRACLE_DURATION = plugin.getConfig().getInt("miracleDuration", 3) * 24000L; // 3 in-game days, 24000 ticks per day
        long effectCheckInterval = plugin.getConfig().getLong("effect-interval", 1800) * 20L; // Convert seconds to ticks

        // Decay system variables
        this.decayRate = plugin.getConfig().getInt("decay-rate", 5);
        this.decayInterval = plugin.getConfig().getLong("decay-interval", 1200) * 20L; // Default 1200 seconds (20 minutes) converted to ticks
        this.lastDecayTime = System.currentTimeMillis();

        // Check for effects (Blessings, Curses, Miracles)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkForEffects, 0L, effectCheckInterval);

        // Check for favor decay (if player hasn't worshipped deity in too long)
        Bukkit.getScheduler().runTaskTimer(plugin, this::decayFavor, 0L, 300L * 20L); // Check every 5 minutes
    }

    private void checkForEffects() {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) { // Check if the player is online first
            long currentTime = System.currentTimeMillis();
            List<String> possibleEffects = new ArrayList<>();

            // Check for blessings
            if (favor >= BLESSING_THRESHOLD && Math.random() < BLESSING_CHANCE) {
                possibleEffects.add("blessing");
            }

            // Check for curses
            if (favor <= CURSE_THRESHOLD && Math.random() < CURSE_CHANCE) {
                possibleEffects.add("curse");
            }

            // Check for miracles
            if (favor >= MIRACLE_THRESHOLD && (lastTimeBelowMiracleThreshold == 0 ||
                    (currentTime - lastTimeBelowMiracleThreshold) >= MIRACLE_DURATION) &&
                    Math.random() < MIRACLE_CHANCE) {
                possibleEffects.add("miracle");
            }

            // Apply only one effect
            if (!possibleEffects.isEmpty()) {
                Collections.shuffle(possibleEffects); // Randomize the list
                String selectedEffect = possibleEffects.get(0); // Get the first (random) effect

                switch (selectedEffect) {
                    case "blessing" -> deity.applyBlessing(player, deity);
                    case "curse" -> deity.applyCurse(player, deity);
                    case "miracle" -> {
                        deity.applyMiracle(player);
                        lastTimeBelowMiracleThreshold = 0; // Reset timer for miracles
                    }
                }
            }

            // Update timer for miracles if not selected
            if (!possibleEffects.contains("miracle")) {
                lastTimeBelowMiracleThreshold = currentTime;
            }
        }
    }


    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public UUID getPlayerUUID() { return uuid; }

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
            Component favorMessage = MessageUtils.getFavorText(this.favor);
            player.sendMessage(Component.text("§aYour favor with " + deity.getName() + " has increased to ").append(favorMessage));

            DevotionStorage devotionStorage = plugin.getDevotionStorage();
            devotionStorage.savePlayerDevotion(player.getUniqueId(), this);
        } else {
            System.err.println("Error: Player or Deity is null in PlayerDevotion::increaseFavor");
        }
    }

    public void decreaseFavor(int amount) {
        // Ensure favor is 0 at the lowest
        if (this.favor <= 0 ) {
            this.favor = 0;
            // Skip processing if player's favor is already 0
            return;
        }

        this.favor -= amount;
        if (this.favor < 0) {
            this.favor = 0;
        }

        Player player = getPlayer();
        if (player != null && player.isOnline() && deity != null) {
            Component favorMessage = MessageUtils.getFavorText(this.favor);
            player.sendMessage(Component.text("§cYour favor with " + deity.getName() + " has decreased to ").append(favorMessage));
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