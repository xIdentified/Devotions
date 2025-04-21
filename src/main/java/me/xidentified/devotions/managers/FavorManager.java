package me.xidentified.devotions.managers;

import de.cubbossa.tinytranslations.libs.kyori.adventure.text.ComponentLike;
import de.cubbossa.tinytranslations.libs.kyori.adventure.text.minimessage.tag.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.util.FavorUtils;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

// Point system for tracking favor with each deity
public class FavorManager {

    @Getter private final Devotions plugin;
    @Getter private final UUID uuid;
    @Getter @Setter private Deity deity;
    @Getter @Setter private int favor;
    @Getter private final int maxFavor;

    // Thresholds & chances
    private final int BLESSING_THRESHOLD;
    private final int CURSE_THRESHOLD;
    private final double BLESSING_CHANCE;
    private final double CURSE_CHANCE;
    private final int MIRACLE_THRESHOLD;
    private final double MIRACLE_CHANCE;
    private final long MIRACLE_DURATION;
    private long lastTimeBelowMiracleThreshold;

    // Decay logic
    private final int decayRate;
    private int decayTaskId = -1;
    private int effectTaskId = -1;

    // Update the constructor to use deity-specific settings
    public FavorManager(Devotions plugin, UUID playerUUID, Deity deity) {
        this.plugin = plugin;
        this.uuid = playerUUID;
        this.deity = deity;

        // Use deity-specific values instead of global ones
        this.favor = deity.getInitialFavor();
        this.maxFavor = deity.getMaxFavor();

        // Thresholds & chances from deity
        this.BLESSING_THRESHOLD = deity.getBlessingThreshold();
        this.CURSE_THRESHOLD = deity.getCurseThreshold();
        this.BLESSING_CHANCE = deity.getBlessingChance();
        this.CURSE_CHANCE = deity.getCurseChance();
        this.MIRACLE_THRESHOLD = deity.getMiracleThreshold();
        this.MIRACLE_CHANCE = deity.getMiracleChance();

        // 3 in-game days by default (3 * 24000 ticks = 72000 ticks)
        this.MIRACLE_DURATION = plugin.getConfig().getInt("miracleDuration", 3) * 24000L;

        long effectCheckInterval = plugin.getConfig().getLong("effect-interval", 1800) * 20L; // in ticks

        // Decay config - use deity-specific decay rate
        this.decayRate = deity.getFavorDecayRate();
        // e.g. 1200 seconds = 20 minutes -> * 20 => 24000 ticks
        long decayInterval = plugin.getConfig().getLong("decay-interval", 1200) * 20L;

        // Start repeated checks for blessings/curses/miracles
        this.effectTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin, this::checkForEffects, 0L, effectCheckInterval).getTaskId();

        // Decay on the exact interval from config.
        this.decayTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin, this::decayFavor, decayInterval, decayInterval).getTaskId();
    }

    /**
     * Check for blessings, curses, or miracles.
     */
    private void checkForEffects() {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            long currentTime = System.currentTimeMillis();
            List<String> effects = new ArrayList<>();

            // Blessing
            if (favor >= BLESSING_THRESHOLD && Math.random() < BLESSING_CHANCE) {
                effects.add("blessing");
            }
            // Curse
            if (favor <= CURSE_THRESHOLD && Math.random() < CURSE_CHANCE) {
                effects.add("curse");
            }
            // Miracle
            boolean canDoMiracle = (lastTimeBelowMiracleThreshold == 0
                    || (currentTime - lastTimeBelowMiracleThreshold) >= MIRACLE_DURATION);
            if (favor >= MIRACLE_THRESHOLD && canDoMiracle && Math.random() < MIRACLE_CHANCE) {
                effects.add("miracle");
            }

            if (!effects.isEmpty()) {
                // Pick one effect at random
                Collections.shuffle(effects);
                String chosen = effects.get(0);

                switch (chosen) {
                    case "blessing" -> deity.applyBlessing(player, deity);
                    case "curse" -> deity.applyCurse(player, deity);
                    case "miracle" -> {
                        deity.applyMiracle(player);
                        lastTimeBelowMiracleThreshold = 0;
                    }
                }
            }
            // If we did not trigger a miracle, track the time
            if (!effects.contains("miracle")) {
                lastTimeBelowMiracleThreshold = currentTime;
            }
        }
    }

    // Update the adjustFavor method to use personality-based adjustments
    public void adjustFavor(int amount) {
        // Apply personality-based adjustment to the favor amount
        int adjustedAmount = deity.adjustFavorByPersonality(amount);

        this.favor += adjustedAmount;
        if (this.favor < 0) {
            this.favor = 0;
        } else if (this.favor > maxFavor) {
            this.favor = maxFavor;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && deity != null) {
            // Only send a message if favor actually changed
            if (adjustedAmount != 0) {
                ComponentLike message = (adjustedAmount > 0)
                        ? Messages.FAVOR_INCREASED
                        .insertParsed("deity", deity.getName())
                        .insertString("favor", String.valueOf(this.favor))
                        .insertTag("favor_col", Tag.styling(s -> s.color(FavorUtils.getColorForFavor(this.favor))))
                        : Messages.FAVOR_DECREASED
                                .insertParsed("deity", deity.getName())
                                .insertParsed("favor", String.valueOf(this.favor))
                                .insertTag("favor_col", Tag.styling(s -> s.color(FavorUtils.getColorForFavor(this.favor))));

            if (!plugin.getDevotionsConfig().isHideFavorMessages()) {
                Devotions.sendMessage(player, message);
            }
        }
    }
    plugin.getStorageManager().getStorage().savePlayerDevotion(uuid, this);
}

    /**
     * Called automatically on the schedule to reduce favor by decayRate.
     */
    private void decayFavor() {
        Player player = Bukkit.getPlayer(uuid);
        boolean decayWhenOffline = plugin.getConfig().getBoolean("decay-when-offline", false);

        // If not decaying offline, skip for offline player
        if (!decayWhenOffline && (player == null || !player.isOnline())) {
            return;
        }
        // If already at 0 favor, no need to decay further
        if (this.favor <= 0) {
            return;
        }
        // Decay the favor
        adjustFavor(-decayRate);
    }

    /**
     * Resets favor to the plugin's initial-favor value.
     */
    public void resetFavor() {
        this.favor = plugin.getConfig().getInt("initial-favor");
    }

    /**
     * Cancel the scheduled tasks for this FavorManager (called when removing devotion).
     */
    public void cancelTasks() {
        if (decayTaskId != -1) {
            Bukkit.getScheduler().cancelTask(decayTaskId);
            decayTaskId = -1;
        }
        if (effectTaskId != -1) {
            Bukkit.getScheduler().cancelTask(effectTaskId);
            effectTaskId = -1;
        }
    }
}
