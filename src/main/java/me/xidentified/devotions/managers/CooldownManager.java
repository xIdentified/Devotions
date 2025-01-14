package me.xidentified.devotions.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.xidentified.devotions.Devotions;
import org.bukkit.entity.Player;

public class CooldownManager {

    private final Devotions plugin;
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public CooldownManager(Devotions plugin) {
        this.plugin = plugin;
        loadCooldownsFromConfig();
    }

    private void loadCooldownsFromConfig() {
        cooldowns.put("blessing", getCooldownFromConfig("blessing-cooldown", "10m"));
        cooldowns.put("curse", getCooldownFromConfig("curse-cooldown", "30m"));
        cooldowns.put("ritual", getCooldownFromConfig("ritual-cooldown", "3s"));
        cooldowns.put("offering", getCooldownFromConfig("offering-cooldown", "3s"));
        cooldowns.put("miracle", getCooldownFromConfig("miracle-cooldown", "1d"));
        cooldowns.put("shrine", getCooldownFromConfig("shrine-cooldown", "3s"));
    }

    /**
     * Sets an action cooldown for a specific player in milliseconds.
     */
    public void setCooldown(Player player, String action, long cooldownTimeMs) {
        Map<String, Long> playerTimestamps = playerCooldowns
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        playerTimestamps.put(action, System.currentTimeMillis() + cooldownTimeMs);
    }

    /**
     * Checks if a player's action is allowed. Returns 0 if allowed,
     * or the remaining milliseconds until it can be performed again.
     */
    public long isActionAllowed(Player player, String action) {
        long currentTime = System.currentTimeMillis();
        Map<String, Long> playerTimestamps = playerCooldowns.get(player.getUniqueId());

        if (playerTimestamps == null) {
            return 0; // No record of cooldowns for this player
        }

        Long nextAllowedTime = playerTimestamps.get(action);
        if (nextAllowedTime != null && currentTime < nextAllowedTime) {
            return nextAllowedTime - currentTime;
        }

        return 0; // No cooldown or it has expired
    }

    /**
     * Parse a string like "1d2h10m" into milliseconds.
     */
    public long parseCooldown(String input) {
        Pattern pattern = Pattern.compile("(\\d+)([dhms])");
        Matcher matcher = pattern.matcher(input);
        long totalMillis = 0;

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2)) {
                case "d" -> totalMillis += TimeUnit.DAYS.toMillis(value);
                case "h" -> totalMillis += TimeUnit.HOURS.toMillis(value);
                case "m" -> totalMillis += TimeUnit.MINUTES.toMillis(value);
                case "s" -> totalMillis += TimeUnit.SECONDS.toMillis(value);
            }
        }
        return totalMillis;
    }

    /**
     * Load a cooldown value from config, parse it to milliseconds.
     * If not found, use defaultValue (like "10m").
     */
    public long getCooldownFromConfig(String path, String defaultValue) {
        String cooldownStr = plugin.getConfig().getString(path, defaultValue);
        return parseCooldown(cooldownStr);
    }

    /**
     * Completely removes all cooldown data for a specific player.
     */
    public void clearCooldowns(Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }
}