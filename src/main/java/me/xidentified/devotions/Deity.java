package me.xidentified.devotions;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.Getter;
import me.xidentified.devotions.effects.Blessing;
import me.xidentified.devotions.effects.Curse;
import me.xidentified.devotions.managers.CooldownManager;
import me.xidentified.devotions.rituals.RitualManager;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Deity {

    private final Devotions plugin;
    // Getter methods below
    @Getter
    public final String name;
    @Getter
    private final String lore;
    @Getter
    private final String alignment;
    private final String domain;
    @Getter
    private final List<Offering> offerings;
    private final List<String> rituals;
    private final List<Blessing> blessings;
    private final List<Curse> curses;
    private final List<Miracle> miracles;
    @Getter final String abandonCondition;
    @Getter private final String selectionCondition;
    @Getter private final int blessingThreshold;
    @Getter private final int curseThreshold;
    @Getter private final int miracleThreshold;
    @Getter private final double blessingChance;
    @Getter private final double curseChance;
    @Getter private final double miracleChance;
    @Getter private final int favorDecayRate;
    @Getter private final String personality;
    @Getter private final int initialFavor;
    @Getter private final int maxFavor;

    public Deity(Devotions plugin, String name, String lore, String domain, String alignment,
            List<Offering> offerings, List<String> rituals, List<Blessing> blessings,
            List<Curse> curses, List<Miracle> miracles, String abandonCondition,
            String selectionCondition, int blessingThreshold, int curseThreshold,
            int miracleThreshold, double blessingChance, double curseChance,
            double miracleChance, int favorDecayRate, String personality,
            int initialFavor, int maxFavor) {
        this.plugin = plugin;
        this.name = name;
        this.lore = lore;
        this.domain = domain;
        this.alignment = alignment;
        this.offerings = offerings;
        this.rituals = rituals;
        this.blessings = blessings;
        this.curses = curses;
        this.miracles = miracles;
        this.abandonCondition = abandonCondition;
        this.selectionCondition = selectionCondition;

        // Initialize new fields
        this.blessingThreshold = blessingThreshold;
        this.curseThreshold = curseThreshold;
        this.miracleThreshold = miracleThreshold;
        this.blessingChance = blessingChance;
        this.curseChance = curseChance;
        this.miracleChance = miracleChance;
        this.favorDecayRate = favorDecayRate;
        this.personality = personality;
        this.initialFavor = initialFavor;
        this.maxFavor = maxFavor;
    }

    private CooldownManager cooldownManager() {
        return plugin.getCooldownManager();
    }

    public void applyBlessing(Player player, Deity deity) {
        if (blessings.isEmpty()) {
            return;
        }

        long remainingCooldown = cooldownManager().isActionAllowed(player, "blessing");
        if (remainingCooldown <= 0) {
            // Randomly select a blessing
            Blessing blessing = blessings.get(new Random().nextInt(blessings.size()));

            // Apply the blessing
            blessing.applyTo(player);
            blessing.applyVisualEffect(player);
            blessing.applyAudioEffect(player);

            // Start cooldown
            long blessingCooldown = cooldownManager().getCooldownFromConfig("blessing-cooldown", "5s");
            plugin.getCooldownManager().setCooldown(player, "blessing", blessingCooldown);

            // Provide feedback
            plugin.debugLog("Blessing applied to player " + player.getName());
            Devotions.sendMessage(player, Messages.DEITY_BLESSED
                    .insertString("deity", deity.getName())
                    .insertString("blessing", blessing.getName())
            );
        }
    }

    public void applyCurse(Player player, Deity deity) {
        if (curses.isEmpty()) {
            return;
        }

        long remainingCooldown = cooldownManager().isActionAllowed(player, "curse");
        if (remainingCooldown <= 0) {
            // Randomly select a curse
            Curse curse = curses.get(new Random().nextInt(curses.size()));

            // Apply the curse
            curse.applyTo(player);
            curse.applyVisualEffect(player);
            curse.applyAudioEffect(player);

            // Start cooldown
            long curseCooldown = cooldownManager().getCooldownFromConfig("curse-cooldown", "5s");
            plugin.getCooldownManager().setCooldown(player, "curse", curseCooldown);

            // Provide feedback
            plugin.debugLog("Curse applied to player " + player.getName());
            Devotions.sendMessage(player, Messages.DEITY_CURSED
                    .insertParsed("deity", deity.getName())
                    .insertParsed("curse", curse.getName())
            );
        }
    }

    public void applyMiracle(Player player) {
        if (miracles.isEmpty()) {
            return;
        }

        Miracle selectedMiracle = selectMiracleForPlayer(player);
        if (selectedMiracle != null) {
            selectedMiracle.apply(player);
            plugin.debugLog("Miracle " + selectedMiracle.getName() + " applied to player " + player.getName());
        }
    }

    private Miracle selectMiracleForPlayer(Player player) {
        // Log the total number of miracles before filtering
        plugin.debugLog("Total miracles: " + miracles.size());

        List<Miracle> applicableMiracles = miracles.stream()
                .filter(miracle -> {
                    boolean canTrigger = miracle.canTrigger(player);
                    // Log the names of each miracle that passes the canTrigger check
                    if (canTrigger) {
                        plugin.debugLog("Miracle " + miracle.getName() + " can be applied.");
                    } else {
                        plugin.debugLog("Miracle " + miracle.getName() + " cannot be applied.");
                    }
                    return canTrigger;
                })
                .toList();

        // Log the total number of applicable miracles after filtering
        plugin.debugLog("Number of applicable miracles: " + applicableMiracles.size());

        if (applicableMiracles.isEmpty()) {
            return null;  // No miracles can be applied
        }

        // Randomly select from applicable miracles
        int randomIndex = new Random().nextInt(applicableMiracles.size());
        return applicableMiracles.get(randomIndex);
    }

    public CharSequence getDomain() {
        return this.domain;
    }

    // Return offerings as a well formatted list
    public String getFormattedOfferings() {
        return offerings.stream()
                .map(offering -> {
                    ItemStack itemStack = offering.getItemStack();
                    String[] parts = itemStack.getType().name().split("_");
                    return Arrays.stream(parts)
                            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                            .collect(Collectors.joining(" "));
                })
                .collect(Collectors.joining(", "));
    }

    // Return rituals as a well formatted list
    public String getRituals() {
        return rituals.stream()
                .map(ritualKey -> {
                    // Retrieve the ritual using the configuration name (ritualKey)
                    Ritual ritual = RitualManager.getInstance(plugin).getRitualByKey(ritualKey);
                    // Debug log to check if ritual is not null and what getDisplayName returns
                    plugin.debugLog(
                            "Ritual Key: " + ritualKey + ", Ritual Found: " + (ritual != null) + ", Display Name: " + (
                                    ritual != null ? ritual.getDisplayName() : "N/A"));
                    // Return the display name if the ritual is found, otherwise return the key itself
                    return ritual != null ? ritual.getDisplayName() : ritualKey;
                })
                .collect(Collectors.joining(", "));
    }

    public List<String> getRitualKeys() {
        return rituals;
    }

    public int adjustFavorByPersonality(int favorChange) {
        switch (personality.toLowerCase()) {
            case "vengeful":
                // Vengeful deities punish more harshly for negative actions
                if (favorChange < 0) {
                    return (int) (favorChange * 1.5);
                }
                break;
            case "forgiving":
                // Forgiving deities don't punish as harshly
                if (favorChange < 0) {
                    return (int) (favorChange * 0.7);
                }
                break;
            case "generous":
                // Generous deities reward more for positive actions
                if (favorChange > 0) {
                    return (int) (favorChange * 1.3);
                }
                break;
            case "demanding":
                // Demanding deities give less reward for positive actions
                if (favorChange > 0) {
                    return (int) (favorChange * 0.8);
                }
                break;
            // Default case - no adjustment
        }
        return favorChange;
    }
}
