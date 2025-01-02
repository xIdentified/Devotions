package me.xidentified.devotions.managers;

import static org.bukkit.Bukkit.getServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<UUID, FavorManager> playerDevotions = new ConcurrentHashMap<>();
    private final Map<String, Deity> deities;
    @Getter @Setter private Map<UUID, Set<String>> abandonedDeities = new ConcurrentHashMap<>();

    public DevotionManager(Devotions plugin, Map<String, Deity> loadedDeities) {
        this.plugin = plugin;
        this.deities = loadedDeities;
        this.storage = plugin.getStorageManager();
        loadPlayerDevotions();
    }

    public List<FavorManager> getSortedFavorDataByDeity(Deity deity) {
        return playerDevotions.values().stream()
                .filter(favorManager -> favorManager.getDeity().equals(deity))
                .sorted(Comparator.comparingInt(FavorManager::getFavor).reversed())
                .collect(Collectors.toList());
    }

    public Deity getDeityByInput(String input) {
        if (input == null || deities == null) {
            return null;
        }

        String normalizedInput = input.toLowerCase();

        // Try to match by internal ID (key in the map)
        Deity deity = deities.get(normalizedInput);
        if (deity != null) {
            return deity;
        }

        // Try to match by display name (case-insensitive)
        for (Deity d : deities.values()) {
            if (d.getName().equalsIgnoreCase(input)) {
                return d;
            }
        }

        return null; // No match found
    }


    public FavorManager getPlayerDevotion(UUID playerUUID) {
        FavorManager manager = playerDevotions.get(playerUUID);
        if (manager == null || manager.getDeity() == null) {
            plugin.debugLog("No Devotion found for UUID " + playerUUID);
            return null;
        }
        plugin.debugLog("Retrieved Devotion for UUID " + playerUUID + ": " + manager.getDeity().getName());
        return manager;
    }

    public synchronized void setPlayerDevotion(UUID playerUUID, FavorManager newDevotion) {
        if (playerUUID == null || newDevotion == null) {
            plugin.getLogger().warning(
                    "Attempted to set null player ID or devotion: Player ID = " + playerUUID + ", Devotion = "
                            + newDevotion);
            return;
        }

        FavorManager currentDevotion;
        synchronized (playerDevotions) {
            currentDevotion = playerDevotions.get(playerUUID);
            if (currentDevotion != null && !currentDevotion.getDeity().equals(newDevotion.getDeity())) {
                // Player is switching to a new deity, reset their favor
                newDevotion.resetFavor();
            }

            // Update the player's devotion with the new or reset FavorManager
            playerDevotions.put(playerUUID, newDevotion);
        }

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
            plugin.getLogger().warning("Tried to set devotion for a player that is not online: " + playerUUID);
        }
    }

    public void removeDevotion(UUID playerUUID) {
        FavorManager favorManager = playerDevotions.remove(playerUUID);
        if (favorManager != null) {
            int abandonedFavor = plugin.getConfig().getInt("abandoned-favor", 0);
            favorManager.setFavor(abandonedFavor);
            storage.getStorage().savePlayerDevotion(playerUUID, favorManager);
        }
        storage.getStorage().removePlayerDevotion(playerUUID);
    }

    public void loadPlayerDevotions() {
        Set<UUID> playerUUIDs = getAllStoredPlayerUUIDs();
        for (UUID uuid : playerUUIDs) {
            DevotionData devotionData = storage.getStorage().getPlayerDevotion(uuid);
            if (devotionData != null) {
                Deity deity = getDeityByInput(devotionData.getDeityName());
                if (deity != null) {
                    FavorManager favorManager = new FavorManager(plugin, uuid, deity);
                    favorManager.setFavor(devotionData.getFavor());
                    playerDevotions.put(uuid, favorManager);
                } else {
                    plugin.getLogger().warning("Deity not found for UUID: " + uuid);
                }
            }
        }
        plugin.getLogger().info("Loaded devotions for " + playerDevotions.size() + " players.");
    }

    private Set<UUID> getAllStoredPlayerUUIDs() {
        return storage.getStorage().getAllStoredPlayerUUIDs();
    }

    public List<Deity> getAllDeities() {
        return new ArrayList<>(deities.values());
    }

    public void saveAllPlayerDevotions() {
        for (Player player : getServer().getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            FavorManager favorManager = playerDevotions.get(playerUUID);
            if (favorManager != null) {
                storage.getStorage().savePlayerDevotion(playerUUID, favorManager);
            }
        }
    }

    public List<FavorManager> getSortedFavorData() {
        List<FavorManager> sortedFavorData = new ArrayList<>(playerDevotions.values());
        // Sort the list by favor in descending order so top players show first
        sortedFavorData.sort((o1, o2) -> Integer.compare(o2.getFavor(), o1.getFavor()));
        return sortedFavorData;
    }

    public void clearData() {
        playerDevotions.clear();
        deities.clear();
    }

    public void reset() {
        playerDevotions.clear();
        loadPlayerDevotions();
    }

    public void markDeityAsAbandoned(UUID playerUUID, String deityId) {
        abandonedDeities.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(deityId.toLowerCase());
    }

    public boolean hasAbandonedDeity(UUID playerUUID, String deityId) {
        Set<String> abandoned = abandonedDeities.get(playerUUID);
        return abandoned != null && abandoned.contains(deityId.toLowerCase());
    }

    public void clearAbandonedDeity(UUID playerUUID, String deityId) {
        Set<String> abandoned = abandonedDeities.get(playerUUID);
        if (abandoned != null) {
            abandoned.remove(deityId.toLowerCase());
        }
    }

}

