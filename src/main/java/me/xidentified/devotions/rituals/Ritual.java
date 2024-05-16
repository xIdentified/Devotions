package me.xidentified.devotions.rituals;

import java.util.List;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.util.JavaScriptEngine;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

@Getter
public class Ritual {

    public boolean isCompleted = false;
    private final Devotions plugin;
    private final DevotionManager devotionManager;
    private final String key;
    private final String displayName;
    private final String description;
    private final RitualItem item;
    private final int favorAmount;
    private final RitualConditions conditions;
    private final RitualOutcome outcome;
    private final List<RitualObjective> objectives;

    public Ritual(Devotions plugin, String key, String displayName, String description, RitualItem item,
            int favorAmount,
            RitualConditions conditions, RitualOutcome outcome, List<RitualObjective> objectives) {
        this.plugin = plugin;
        this.devotionManager = plugin.getDevotionManager();
        this.key = key;
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

        // JavaScript expression condition
        if (!conditions.expression().isEmpty()) {
            // Replace placeholders with actual values
            String parsedExpression = PlaceholderAPI.setPlaceholders(player, conditions.expression());
            plugin.debugLog("Evaluating JavaScript expression condition. Expression: " + parsedExpression);

            // Evaluate the expression using the JavaScript engine
            boolean expressionResult = JavaScriptEngine.evaluateExpression(parsedExpression);
            if (!expressionResult) {
                plugin.debugLog("JavaScript expression condition failed.");
                return false;
            }
        }

        // Time condition
        if (conditions.time() != null) {
            long time = player.getWorld().getTime();
            plugin.debugLog("Checking time condition. Current time: " + time + ", Required: " + conditions.time());
            if (("DAY".equalsIgnoreCase(conditions.time()) && time >= 12300) ||
                    ("NIGHT".equalsIgnoreCase(conditions.time()) && time < 12300)) {
                plugin.debugLog("Time condition failed.");
                return false;
            }
        }

        // Biome condition
        if (conditions.biome() != null) {
            String currentBiome = player.getLocation().getBlock().getBiome().toString();
            plugin.debugLog(
                    "Checking biome condition. Current biome: " + currentBiome + ", Required: " + conditions.biome());
            if (!currentBiome.equalsIgnoreCase(conditions.biome())) {
                plugin.debugLog("Biome condition failed.");
                return false;
            }
        }

        // Weather condition
        if (conditions.weather() != null) {
            boolean isRaining = player.getWorld().hasStorm();
            plugin.debugLog(
                    "Checking weather condition. Is raining: " + isRaining + ", Required: " + conditions.weather());
            if (("RAIN".equalsIgnoreCase(conditions.weather()) && !isRaining) ||
                    ("CLEAR".equalsIgnoreCase(conditions.weather()) && isRaining)) {
                plugin.debugLog("Weather condition failed.");
                return false;
            }
        }

        // Moon phase condition
        if (conditions.moonPhase() != null) {
            long moonPhase = player.getWorld().getFullTime() / 24000L % 8;
            plugin.debugLog("Checking moon phase condition. Current phase: " + moonPhase + ", Required: "
                    + conditions.moonPhase());
            if (moonPhase != conditions.getMoonPhaseNumber(conditions.moonPhase())) {
                plugin.debugLog("Moon phase condition failed.");
                return false;
            }
        }

        // Altitude condition
        if (conditions.minAltitude() != 0.0) {
            double altitude = player.getLocation().getY();
            plugin.debugLog("Checking altitude condition. Current altitude: " + altitude + ", Minimum required: "
                    + conditions.minAltitude());
            if (altitude < conditions.minAltitude()) {
                plugin.debugLog("Altitude condition failed.");
                return false;
            }
        }

        // Experience condition
        if (conditions.minExperience() != 0) {
            int experience = player.getTotalExperience();
            plugin.debugLog("Checking experience condition. Current experience: " + experience + ", Minimum required: "
                    + conditions.minExperience());
            if (experience < conditions.minExperience()) {
                plugin.debugLog("Experience condition failed.");
                return false;
            }
        }

        // Health condition
        if (conditions.minHealth() != 0.0) {
            double health = player.getHealth();
            plugin.debugLog("Checking health condition. Current health: " + health + ", Minimum required: "
                    + conditions.minHealth());
            if (health < conditions.minHealth()) {
                plugin.debugLog("Health condition failed.");
                return false;
            }
        }

        // Hunger condition
        if (conditions.minHunger() != 0) {
            int hunger = player.getFoodLevel();
            plugin.debugLog("Checking hunger condition. Current hunger: " + hunger + ", Minimum required: "
                    + conditions.minHunger());
            if (hunger < conditions.minHunger()) {
                plugin.debugLog("Hunger condition failed.");
                return false;
            }
        }

        plugin.debugLog("All conditions passed.");
        return true;
    }

    public void provideFeedback(Player player, String feedbackType) {
        FavorManager favorManager = devotionManager.getPlayerDevotion(player.getUniqueId());
        Location location = player.getLocation().add(0, 1, 0); // Elevate the location slightly
        switch (feedbackType) {
            case "START" -> {
                plugin.playConfiguredSound(player, "ritualStarted");
                plugin.spawnParticles(location, Particle.ENCHANTMENT_TABLE, 50, 2.0, 0.5);
                Devotions.sendMessage(player, Messages.RITUAL_START.insertParsed("ritual", displayName));
            }
            case "SUCCESS" -> {
                plugin.playConfiguredSound(player, "ritualComplete");
                plugin.spawnParticles(location, Particle.VILLAGER_HAPPY, 50, 2.0, 0.5);
                Devotions.sendMessage(player, Messages.RITUAL_SUCCESS.insertParsed("ritual", displayName));
                favorManager.adjustFavor(favorAmount);
            }
            case "FAILURE" -> {
                plugin.playConfiguredSound(player, "ritualFailed");
                plugin.spawnParticles(location, Particle.REDSTONE, 50, 2.0, 0.5);
                player.damage(5);
                Devotions.sendMessage(player, Messages.RITUAL_FAILURE.insertParsed("ritual", displayName));
                favorManager.adjustFavor(-15); // TODO: Make this a certain percent of the favorAmount maybe??
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
            if (!word.isEmpty()) {
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

