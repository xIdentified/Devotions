package me.xidentified.devotions.rituals;

import lombok.Getter;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.util.MessageUtils;
import me.xidentified.devotions.util.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.List;

public class Ritual {
    public boolean isCompleted = false;
    private final Devotions plugin;
    private final DevotionManager devotionManager;
    @Getter
    private final String displayName;
    @Getter
    private final String description;
    @Getter
    private final RitualItem item;
    @Getter
    private final int favorAmount;
    private final RitualConditions conditions;
    @Getter
    private final RitualOutcome outcome;
    @Getter
    private final List<RitualObjective> objectives;

    public Ritual(Devotions plugin, String displayName, String description, RitualItem item, int favorAmount,
                  RitualConditions conditions, RitualOutcome outcome, List<RitualObjective> objectives) {
        this.plugin = plugin;
        this.devotionManager = plugin.getDevotionManager();
        this.displayName = displayName;
        this.description = description;
        this.item = item;
        this.favorAmount = favorAmount;
        this.conditions = conditions;
        this.outcome = outcome;
        this.objectives = objectives;
    }

    // Validate ritual conditions against a player and environment.
    public boolean validateConditions(Player player) {
        plugin.debugLog("Validating Conditions for ritual " + this.getDisplayName());

        // Time condition
        if (conditions.time() != null) {
            long time = player.getWorld().getTime();
            if (("DAY".equalsIgnoreCase(conditions.time()) && time >= 12300) ||
                    ("NIGHT".equalsIgnoreCase(conditions.time()) && time < 12300)) {
                return false;
            }
        }

        // Biome condition
        if (conditions.biome() != null && !player.getLocation().getBlock().getBiome().toString().equalsIgnoreCase(conditions.biome())) {
            return false;
        }

        // Weather condition
        if (conditions.weather() != null) {
            boolean isRaining = player.getWorld().hasStorm();
            if (("RAIN".equalsIgnoreCase(conditions.weather()) && !isRaining) ||
                    ("CLEAR".equalsIgnoreCase(conditions.weather()) && isRaining)) {
                return false;
            }
        }

        // Moon phase condition
        if (conditions.moonPhase() != null && player.getWorld().getFullTime() / 24000L % 8 != conditions.getMoonPhaseNumber(conditions.moonPhase())) {
            return false;
        }

        // Altitude condition
        if (conditions.minAltitude() != 0.0 && player.getLocation().getY() < conditions.minAltitude()) {
            return false;
        }

        // Experience condition
        if (conditions.minExperience() != 0 && player.getTotalExperience() < conditions.minExperience()) {
            return false;
        }

        // Health condition
        if (conditions.minHealth() != 0.0 && player.getHealth() < conditions.minHealth()) {
            return false;
        }

        // Hunger condition
        if (conditions.minHunger() != 0 && player.getFoodLevel() < conditions.minHunger()) {
            return false;
        }

        // All conditions are met
        return true;
    }


    public void provideFeedback(Player player, String feedbackType) {
        FavorManager favorManager = devotionManager.getPlayerDevotion(player.getUniqueId());
        Location location = player.getLocation().add(0, 1, 0); // Elevate the location slightly
        switch (feedbackType) {
            case "START" -> {
                plugin.playConfiguredSound(player, "ritualStarted");
                plugin.spawnParticles(location, Particle.ENCHANTMENT_TABLE, 50, 2.0, 0.5);
                plugin.sendMessage(player, Messages.RITUAL_START.formatted(Placeholder.unparsed("ritual", displayName)));
            }
            case "SUCCESS" -> {
                plugin.playConfiguredSound(player, "ritualComplete");
                plugin.spawnParticles(location, Particle.VILLAGER_HAPPY, 50, 2.0, 0.5);
                plugin.sendMessage(player, Messages.RITUAL_SUCCESS.formatted(Placeholder.unparsed("ritual", displayName)));
                favorManager.increaseFavor(25);
            }
            case "FAILURE" -> {
                plugin.playConfiguredSound(player, "ritualFailed");
                plugin.spawnParticles(location, Particle.REDSTONE, 50, 2.0, 0.5);
                player.damage(5);
                plugin.sendMessage(player, Messages.RITUAL_FAILURE.formatted(Placeholder.unparsed("ritual", displayName)));
                favorManager.decreaseFavor(15);
            }
        }
    }

    public boolean isCompleted() {
        return this.isCompleted;
    }

    public String getParsedItemName() {
        String id = item.id();
        if (id == null || id.isEmpty()) {
            return "";
        }
        StringBuilder readableName = new StringBuilder();
        String[] words = id.split("_");
        for (String word : words) {
            if (word.length() > 0) {
                readableName.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return readableName.toString().trim();
    }

    public void reset() {
        for (RitualObjective objective : this.getObjectives()) {
            objective.reset();
        }
        this.isCompleted = false;
    }

}

