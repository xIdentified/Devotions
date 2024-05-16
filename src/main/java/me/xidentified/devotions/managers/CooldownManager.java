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

    private final Map<UUID, Map<String, Long>> playerActionTimestamps = new HashMap<>();


    public long isActionAllowed(Player player, String action) {
        long currentTime = System.currentTimeMillis();
        Long cooldownTime = cooldowns.get(action);
        if (cooldownTime == null) {
            return 0; // No cooldown defined for this action
        }

        Map<String, Long> playerTimestamps = playerActionTimestamps.computeIfAbsent(player.getUniqueId(),
                k -> new HashMap<>());
        Long nextAllowedActionTime = playerTimestamps.get(action);

        if (nextAllowedActionTime != null && currentTime < nextAllowedActionTime) {
            return nextAllowedActionTime - currentTime;
        }

        playerTimestamps.put(action, currentTime + cooldownTime);
        return 0;
    }

    // Method to set cooldowns from the config
    public void setCooldown(Player player, String action, long cooldownTimeMs) {
        playerCooldowns
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(action, System.currentTimeMillis() + cooldownTimeMs);
    }

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

    public long getCooldownFromConfig(String path, String defaultValue) {
        String cooldownStr = plugin.getConfig().getString(path, defaultValue);
        return parseCooldown(cooldownStr);
    }

}
