package me.xidentified.devotions.managers;

import io.lumine.mythic.lib.api.item.NBTItem;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.rituals.Ritual;
import me.xidentified.devotions.rituals.RitualItem;
import me.xidentified.devotions.rituals.RitualObjective;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RitualManager {
    private final Devotions plugin;
    private static volatile RitualManager instance; // Use this instance throughout plugin
    public final ConcurrentHashMap<String, Ritual> rituals; // Defined rituals
    private final Map<Player, Ritual> playerRituals = new HashMap<>(); // Track what ritual each player is doing
    public final Map<Player, Item> ritualDroppedItems = new HashMap<>(); // Track dropped item so we can remove later

    public RitualManager(Devotions plugin) {
        this.plugin = plugin;
        rituals = new ConcurrentHashMap<>();
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
        plugin.debugLog("Inside startRitual method");

        // Make sure player isn't already in a ritual before starting another one
        if (RitualManager.getInstance(plugin).getCurrentRitualForPlayer(player) != null) return false;

        // Retrieve the ritual associated with the item
        Ritual ritual = RitualManager.getInstance(plugin).getRitualByItem(item);
        plugin.debugLog("Ritual retrieved: " + ritual.getDisplayName() + ritual.getDescription() + ritual.getFavorAmount() + ritual.getObjectives());

        ritual.reset();
        plugin.debugLog("After reset: " + ritual.getDisplayName() + ritual.getDescription() + ritual.getFavorAmount() + ritual.getObjectives());
        associateDroppedItem(player, droppedItem);

        // Validate the ritual and its conditions
        if (ritual.validateConditions(player)) {
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
                    player.sendMessage(objective.getDescription());
                }
            }

            // Set the ritual for the player, so we can track it
            RitualManager.getInstance(plugin).setRitualForPlayer(player, ritual);
            return true; // Ritual started successfully
        } else {
            if (droppedItem != null) droppedItem.remove();
            ritual.provideFeedback(player, "FAILURE");
            return false; // Ritual did not start
        }
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

    public Ritual getCurrentRitualForPlayer(Player player) {
        return playerRituals.get(player);
    }

    public void setRitualForPlayer(Player player, Ritual ritual) {
        playerRituals.put(player, ritual);
    }

    public void removeRitualForPlayer(Player player) {
        playerRituals.remove(player);
    }

    public Ritual getRitualByItem(ItemStack item) {
        if (item == null) return null;

        String itemId = getItemId(item);
        plugin.debugLog("Looking for ritual associated with item ID " + itemId);

        for (Ritual ritual : rituals.values()) {
            RitualItem keyRitualItem = ritual.getItem();
            if (keyRitualItem != null && keyRitualItem.getUniqueId().equals(itemId)) {
                return ritual;
            }
        }
        return null;
    }


    // Translates item ID from config to match ritual ID in rituals table
    private String getItemId(ItemStack item) {
        String computedId;

        plugin.debugLog("Checking for MMOItem ritual key");
        if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            NBTItem nbtItem = NBTItem.get(item);
            if (nbtItem.hasType()) {
                // constructs an MMOItem ID
                computedId = "MMOITEM:" + nbtItem.getType() + ":" + nbtItem.getString("MMOITEMS_ITEM_ID");
            } else {
                // constructs vanilla item IDs
                computedId = "VANILLA:" + item.getType().name();
            }
        } else {
            plugin.debugLog("Checking for vanilla item ritual key");
            // constructs vanilla item IDs
            computedId = "VANILLA:" + item.getType().name();
        }

        return computedId;
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
        removeRitualForPlayer(player);
        meditationManager.cancelMeditationTimer(player);
        meditationManager.clearMeditationData(player);
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