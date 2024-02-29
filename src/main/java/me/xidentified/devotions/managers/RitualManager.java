package me.xidentified.devotions.managers;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.rituals.RitualItem;
import me.xidentified.devotions.rituals.RitualObjective;
import me.xidentified.devotions.util.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RitualManager {
    private final Devotions plugin;
    private static volatile RitualManager instance; // Use this instance throughout plugin
    public final Map<String, Ritual> rituals; // Defined rituals
    private final Map<Player, Ritual> playerRituals = new HashMap<>(); // Track what ritual each player is doing
    public final Map<Player, Item> ritualDroppedItems = new HashMap<>(); // Track dropped item so we can remove later

    public RitualManager(Devotions plugin) {
        this.plugin = plugin;
        rituals = new HashMap<>();
    }

    // Use one instance of RitualManager throughout the plugin!
    public static RitualManager getInstance(Devotions plugin) {
        if (instance == null) {
            synchronized (RitualManager.class) {
                if (instance == null) {
                    instance = new RitualManager(plugin);
                    plugin.debugLog("New RitualManager instance initialized.");
                }
            }
        }
        return instance;
    }

    public Ritual getRitualByKey(String ritualKey) {
        plugin.debugLog("Attempting to get ritual with key: " + ritualKey);
        Ritual ritual = rituals.get(ritualKey);

        if (ritual != null) {
            plugin.debugLog("Found ritual: " + ritual.getDisplayName());
        } else {
            plugin.debugLog("No ritual found for key: " + ritualKey);
        }
        return ritual;
    }

    public List<String> getAllRitualNames() {
        return new ArrayList<>(rituals.keySet());
    }

    public boolean startRitual(Player player, ItemStack item, Item droppedItem) {
        // Make sure player isn't already in a ritual before starting another one
        if (RitualManager.getInstance(plugin).getCurrentRitualForPlayer(player) != null) return false;

        // Retrieve the ritual associated with the item
        Ritual ritual = RitualManager.getInstance(plugin).getRitualByItem(item);
        plugin.debugLog("Ritual retrieved: " + ritual.getDisplayName() + ritual.getDescription() + ritual.getFavorAmount() + ritual.getObjectives());

        // Retrieve the player's current deity
        FavorManager favorManager = plugin.getDevotionManager().getPlayerDevotion(player.getUniqueId());
        Deity playerDeity = favorManager != null ? favorManager.getDeity() : null;
        if (playerDeity == null || !playerDeity.getRitualKeys().contains(ritual.getKey())) {
            plugin.debugLog("Ritual not allowed by chosen deity");
            plugin.sendMessage(player, Messages.RITUAL_WRONG_DEITY.formatted(
                    Placeholder.unparsed("ritual", ritual.getDisplayName())
            ));
            return false;
        }

        ritual.reset();
        associateDroppedItem(player, droppedItem);

        // Validate the ritual and its conditions
        if (ritual.validateConditions(player)) {
            try {
                plugin.getShrineListener().takeItemInHand(player, item);
            ritual.provideFeedback(player, "START");

            List<RitualObjective> objectives = ritual.getObjectives(); // Directly fetch from the ritual object

            if (objectives != null) {
                for (RitualObjective objective : objectives) {
                    // If it's a purification ritual, we'll spawn the desired mobs around the player
                    if (objective.getType() == RitualObjective.Type.PURIFICATION) {
                        EntityType entityType = EntityType.valueOf(objective.getTarget());
                        Location playerLocation = player.getLocation();
                        plugin.spawnRitualMobs(playerLocation, entityType, objective.getCount(), 2);
                    }
                    if (objective.getType() == RitualObjective.Type.MEDITATION) {
                        plugin.getMeditationManager().startMeditation(player, ritual, objective);
                    }
                    plugin.sendMessage(player, plugin.getTranslations().process(objective.getDescription()));
                }
            }

            // Set the ritual for the player, so we can track it
            RitualManager.getInstance(plugin).setRitualForPlayer(player, ritual);
            return true; // Ritual started successfully
        } catch (Exception e) {
            plugin.getLogger().severe("Error while starting ritual: " + e.getMessage());
            e.printStackTrace();
            // Remove dropped item if an error occurs during ritual initiation
            if (droppedItem != null) droppedItem.remove();
            // Provide feedback to the player about the failure
            ritual.provideFeedback(player, "FAILURE");
            return false;
        }
    } else if (droppedItem != null) {
        droppedItem.remove();
        ritual.provideFeedback(player, "FAILURE");
        return false; // Ritual did not start
    }
    return false;
}

    public void completeRitual(Player player, Ritual ritual, MeditationManager meditationManager) {
        // Get rid of item on shrine
        Item ritualDroppedItem = getAssociatedDroppedItem(player);
        if (ritualDroppedItem != null) ritualDroppedItem.remove();
        removeDroppedItemAssociation(player);

        // Execute outcome and provide feedback
        ritual.getOutcome().executeOutcome(player);
        ritual.provideFeedback(player, "SUCCESS");

        // Mark ritual as complete and clear data
        ritual.isCompleted = true;
        playerRituals.remove(player);
        meditationManager.cancelMeditationTimer(player);
        meditationManager.clearMeditationData(player);
        ritual.reset();
    }

    public void addRitual(String key, Ritual ritual) {
        if (key == null || key.isEmpty()) {
            plugin.getLogger().warning("Attempted to add ritual with empty or null key.");
            return;
        }
        if (ritual == null) {
            plugin.getLogger().warning("Attempted to add null ritual for key: " + key);
            return;
        }
        rituals.put(key, ritual);
    }

    public void cancelRitualFor(Player player) {
        Ritual ritual = playerRituals.remove(player);

        if (ritual != null) {
            Item droppedItem = ritualDroppedItems.remove(player);
            if (droppedItem != null) {
                droppedItem.remove();
            }
            ritual.reset();
        }
    }

    public Ritual getCurrentRitualForPlayer(Player player) {
        return playerRituals.get(player);
    }

    public void setRitualForPlayer(Player player, Ritual ritual) {
        playerRituals.put(player, ritual);
    }

    public Ritual getRitualByItem(ItemStack item) {
        plugin.debugLog("Itemstack in getRitualbyItem returns: " + item);
        if (item == null) return null;

        String itemId = plugin.getDevotionsConfig().getItemId(item);
        plugin.debugLog("Looking for ritual associated with item ID " + itemId);

        for (Ritual ritual : rituals.values()) {
            RitualItem keyRitualItem = ritual.getItem();
            if (keyRitualItem != null && keyRitualItem.getUniqueId().equals(itemId)) {
                plugin.debugLog("Checking if Ritual item " + keyRitualItem + " equals Item ID " + itemId);
                return ritual;
            }
        }
        return null;
    }

    /**
     * Associates an item frame with the player's current ritual
     *
     * @param player The player performing the ritual.
     * @param droppedItem The associated item frame.
     */
    public void associateDroppedItem(Player player, Item droppedItem) {
        ritualDroppedItems.put(player, droppedItem);
    }

    /**
     * Retrieves the item frame associated with a player's ongoing ritual.
     *
     * @param player The player performing the ritual.
     * @return The associated item frame, or null if none exists.
     */
    public Item getAssociatedDroppedItem(Player player) {
        return ritualDroppedItems.get(player);
    }

    /**
     * Removes the association between a player and an item frame.
     *
     * @param player The player to remove the association for.
     */
    public void removeDroppedItemAssociation(Player player) {
        ritualDroppedItems.remove(player);
    }

}