package me.xidentified.devotions.managers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.storage.StorageManager;
import me.xidentified.devotions.storage.model.DevotionData;
import me.xidentified.devotions.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DevotionManager {

    private final Devotions plugin;
    private final StorageManager storage;
    private final Map<UUID, FavorManager> playerDevotions = new HashMap<>();
    private final Map<String, Deity> deities;
    @Getter @Setter private Map<UUID, Set<String>> abandonedDeities = new HashMap<>();

    public DevotionManager(Devotions plugin, Map<String, Deity> loadedDeities) {
        this.plugin = plugin;
        this.deities = loadedDeities;
        this.storage = plugin.getStorageManager();
        loadPlayerDevotions();
    }

    /**
     * Returns a list of all FavorManagers for a given Deity, sorted by favor descending.
     */
    public List<FavorManager> getSortedFavorDataByDeity(Deity deity) {
        return playerDevotions.values().stream()
                .filter(fm -> fm.getDeity().equals(deity))
                .sorted(Comparator.comparingInt(FavorManager::getFavor).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a Deity by internal map key or display name.
     */
    public Deity getDeityByInput(String input) {
        if (input == null || deities == null) return null;

        String normalized = input.toLowerCase();
        // Match by internal ID
        Deity deity = deities.get(normalized);
        if (deity != null) {
            return deity;
        }

        // Match by display name
        for (Deity d : deities.values()) {
            if (d.getName().equalsIgnoreCase(input)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Get the FavorManager for a player if it exists; null otherwise.
     */
    public FavorManager getPlayerDevotion(UUID playerUUID) {
        FavorManager manager = playerDevotions.get(playerUUID);
        if (manager == null || manager.getDeity() == null) {
            plugin.debugLog("No Devotion found for UUID " + playerUUID);
            return null;
        }
        plugin.debugLog("Retrieved Devotion for UUID " + playerUUID + ": " + manager.getDeity().getName());
        return manager;
    }

    /**
     * Assign a new FavorManager to a player.
     * If the old devotion was to a different deity, reset the new manager's favor.
     */
    public void setPlayerDevotion(UUID playerUUID, FavorManager newDevotion) {
        if (playerUUID == null || newDevotion == null) {
            plugin.getLogger().warning("Attempted to set null player ID or devotion: "
                    + playerUUID + ", " + newDevotion);
            return;
        }

        FavorManager currentDevotion = playerDevotions.get(playerUUID);
        if (currentDevotion != null
                && currentDevotion.getDeity() != null
                && !currentDevotion.getDeity().equals(newDevotion.getDeity())) {
            // Player is switching to a new deity, reset their favor
            newDevotion.resetFavor();
        }

        // Update player's devotion
        playerDevotions.put(playerUUID, newDevotion);

        // Persist and notify if player is online
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            Deity deity = newDevotion.getDeity();
            storage.getStorage().savePlayerDevotion(playerUUID, newDevotion);
            plugin.playConfiguredSound(player, "deitySelected");

            Devotions.sendMessage(player, Messages.DEVOTION_SET
                    .insertParsed("name", deity.getName())
                    .insertNumber("favor", newDevotion.getFavor())
            );
        } else {
            plugin.getLogger().warning("Tried to set devotion for offline player: " + playerUUID);
        }
    }

    /**
     * Remove a player's devotion entirely.
     */
    public void removeDevotion(UUID playerUUID) {
        FavorManager fm = playerDevotions.remove(playerUUID);
        if (fm != null) {
            fm.cancelTasks();
            // Set favor to 'abandoned' amount as punishment for leaving devotion
            int abandonedFavor = plugin.getConfig().getInt("abandoned-favor", 0);
            fm.setFavor(abandonedFavor);

            storage.getStorage().savePlayerDevotion(playerUUID, fm);
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            plugin.getCooldownManager().clearCooldowns(player);
        }
        storage.getStorage().removePlayerDevotion(playerUUID);
    }

    /**
     * Loads all saved devotions from storage into memory.
     */
    public void loadPlayerDevotions() {
        Set<UUID> playerUUIDs = storage.getStorage().getAllStoredPlayerUUIDs();
        for (UUID uuid : playerUUIDs) {
            DevotionData data = storage.getStorage().getPlayerDevotion(uuid);
            if (data != null) {
                Deity deity = getDeityByInput(data.getDeityName());
                if (deity != null) {
                    FavorManager favorManager = new FavorManager(plugin, uuid, deity);
                    favorManager.setFavor(data.getFavor());
                    playerDevotions.put(uuid, favorManager);
                } else {
                    plugin.getLogger().warning("Deity not found for UUID: " + uuid);
                }
            }
        }
        plugin.getLogger().info("Loaded devotions for " + playerDevotions.size() + " players.");
    }

    public List<Deity> getAllDeities() {
        return new ArrayList<>(deities.values());
    }

    /**
     * Saves devotions for all currently online players.
     */
    public void saveAllPlayerDevotions() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            FavorManager fm = playerDevotions.get(uuid);
            if (fm != null) {
                storage.getStorage().savePlayerDevotion(uuid, fm);
            }
        }
    }

    /**
     * Returns a list of all FavorManagers sorted by favor descending.
     */
    public List<FavorManager> getSortedFavorData() {
        List<FavorManager> sorted = new ArrayList<>(playerDevotions.values());
        sorted.sort((a, b) -> Integer.compare(b.getFavor(), a.getFavor()));
        return sorted;
    }

    /**
     * Clears all devotions in memory AND the deity map. Use carefully.
     */
    public void clearData() {
        playerDevotions.clear();
        deities.clear();
    }

    /**
     * Reload from storage, discarding current in-memory devotion data.
     */
    public void reset() {
        playerDevotions.clear();
        loadPlayerDevotions();
    }

    /**
     * Track that a player abandoned a specific deity.
     */
    public void markDeityAsAbandoned(UUID playerUUID, String deityId) {
        abandonedDeities.computeIfAbsent(playerUUID, k -> new HashSet<>())
                .add(deityId.toLowerCase());
    }

    /**
     * Check if player has abandoned this deity before.
     */
    public boolean hasAbandonedDeity(UUID playerUUID, String deityId) {
        Set<String> abandoned = abandonedDeities.get(playerUUID);
        return abandoned != null && abandoned.contains(deityId.toLowerCase());
    }

    /**
     * Clears the "abandoned" status for a deity, if needed.
     */
    public void clearAbandonedDeity(UUID playerUUID, String deityId) {
        Set<String> abandoned = abandonedDeities.get(playerUUID);
        if (abandoned != null) {
            abandoned.remove(deityId.toLowerCase());
        }
    }
}
