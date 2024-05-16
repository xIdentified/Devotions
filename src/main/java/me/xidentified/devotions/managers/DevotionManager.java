package me.xidentified.devotions.managers;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.storage.StorageManager;
import me.xidentified.devotions.storage.model.DevotionData;
import me.xidentified.devotions.util.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.Bukkit.getServer;

public class DevotionManager {
    private final Devotions plugin;
    private final StorageManager storage;
    private final Map<UUID, FavorManager> playerDevotions = new ConcurrentHashMap<>();
    private final Map<String, Deity> deities;

    public DevotionManager(Devotions plugin, Map<String, Deity> loadedDeities) {
        this.plugin = plugin;
        this.deities = loadedDeities;
        this.storage = plugin.getStorageManager();
        loadPlayerDevotions();
    }

    public Deity getDeityByName(String name) {
        if (name == null || deities == null) return null;
        return deities.get(name.toLowerCase());
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
            plugin.getLogger().warning("Attempted to set null player ID or devotion: Player ID = " + playerUUID + ", Devotion = " + newDevotion);
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
        playerDevotions.remove(playerUUID);
        storage.getStorage().removePlayerDevotion(playerUUID);
    }

    public void loadPlayerDevotions() {
        Set<UUID> playerUUIDs = getAllStoredPlayerUUIDs();
        for (UUID uuid : playerUUIDs) {
            try {
                DevotionData devotionData = storage.getStorage().getPlayerDevotion(uuid);
                if (devotionData == null) {
                    continue;
                }
                Deity deity = getDeityByName(devotionData.getDeityName());
                if (deity != null) {
                    FavorManager favorManager = new FavorManager(plugin, uuid, deity);
                    favorManager.setFavor(devotionData.getFavor());
                    playerDevotions.put(uuid, favorManager);
                } else {
                    plugin.getLogger().warning("Deity not found for UUID: " + uuid);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading devotion for UUID " + uuid + ": " + e.getMessage());
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

}

