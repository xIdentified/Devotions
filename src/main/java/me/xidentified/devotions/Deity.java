package me.xidentified.devotions;

import lombok.Getter;
import me.xidentified.devotions.effects.Blessing;
import me.xidentified.devotions.effects.Curse;
import me.xidentified.devotions.managers.CooldownManager;
import me.xidentified.devotions.managers.RitualManager;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Deity {
    private final Devotions plugin;
    // Getter methods below
    @Getter public final String name;
    @Getter private final String lore;
    @Getter private final String alignment;
    private final String domain;
    private final List<ItemStack> offerings;
    private final List<String> rituals;
    private final List<Blessing> blessings;
    private final List<Curse> curses;
    private final List<Miracle> miracles;

    public Deity(Devotions plugin, String name, String lore, String domain, String alignment, List<ItemStack> offerings,
                 List<String> rituals, List<Blessing> blessings, List<Curse> curses, List<Miracle> miracles) {
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
    }

    private CooldownManager cooldownManager() {return plugin.getCooldownManager();}

    public void applyBlessing(Player player, Deity deity) {
        if (blessings.isEmpty()) return;

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
            player.sendMessage(MessageUtils.parse("<green>" + deity.getName() + " has blessed you with " + blessing.getName() + "!"));
        }
    }

    public void applyCurse(Player player, Deity deity) {
        if (curses.isEmpty()) return;

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
            player.sendMessage(MessageUtils.parse("<red>" + deity.getName() + " has cursed you with " + curse.getName() + "!"));
        }
    }

    public void applyMiracle(Player player) {
        if (miracles.isEmpty()) return;

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
    public String getOfferings() {
        plugin.debugLog("getOfferings method called!");
        return offerings.stream()
                .map(itemStack -> {
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
                    plugin.debugLog("Ritual Key: " + ritualKey + ", Ritual Found: " + (ritual != null) + ", Display Name: " + (ritual != null ? ritual.getDisplayName() : "N/A"));
                    // Return the display name if the ritual is found, otherwise return the key itself
                    return ritual != null ? ritual.getDisplayName() : ritualKey;
                })
                .collect(Collectors.joining(", "));
    }

}
