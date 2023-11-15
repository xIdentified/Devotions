package me.xidentified.devotions.managers;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.storage.DevotionData;
import me.xidentified.devotions.storage.DevotionStorage;
import me.xidentified.devotions.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;

public class DevotionManager {
    private final Devotions plugin;
    private final DevotionStorage devotionStorage;
    private final Map<UUID, FavorManager> playerDevotions = new ConcurrentHashMap<>();
    private final Map<String, Deity> deities;

    public DevotionManager(Devotions plugin, Map<String, Deity> loadedDeities) {
        this.plugin = plugin;
        this.deities = loadedDeities;
        this.devotionStorage = plugin.getDevotionStorage();
        loadPlayerDevotions();
    }

    public Deity getDeityByName(String name) {
        if (name == null || deities == null) return null;
        return deities.get(name.toLowerCase()); // Ensure that the deity names in the 'deities' map are all lowercase
    }

    public FavorManager getPlayerDevotion(UUID playerUUID) {
        return playerDevotions.get(playerUUID);
    }

    public void setPlayerDevotion(UUID playerUUID, FavorManager devotion) {
        if (playerUUID == null || devotion == null) {
            // Log for debugging
            plugin.getLogger().warning("Attempted to set null player ID or devotion: Player ID = " + playerUUID + ", Devotion = " + devotion);
            return;
        }

        removeDevotion(playerUUID); // Remove current devotion before setting new one
        playerDevotions.put(playerUUID, devotion);
        Player player = Bukkit.getPlayer(playerUUID);

        if (player != null) {
            Deity deity = devotion.getDeity();
            devotionStorage.savePlayerDevotion(playerUUID, devotion); // Set new devotion
            plugin.playConfiguredSound(player, "deitySelected");
            player.sendMessage(MessageUtils.parse("<green>You are now devoted to " + deity.getName() + ". Your favor is " + devotion.getFavor() + "."));
        } else {
            plugin.getLogger().warning("Tried to set devotion for a player that is not online: " + playerUUID);
        }
    }

    public void removeDevotion(UUID playerUUID) {
        playerDevotions.remove(playerUUID);
        devotionStorage.removePlayerDevotion(playerUUID);
    }

    public void loadPlayerDevotions() {
        Set<UUID> playerUUIDs = getAllStoredPlayerUUIDs();
        for (UUID uuid : playerUUIDs) {
            DevotionData devotionData = devotionStorage.getPlayerDevotion(uuid);
            if (devotionData != null) {
                Deity deity = getDeityByName(devotionData.getDeityName());
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
        ConfigurationSection section = devotionStorage.getYaml().getConfigurationSection("playerdata");
        if (section != null) {
            return section.getKeys(false).stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public List<Deity> getAllDeities() {
        return new ArrayList<>(deities.values());
    }

    public void saveAllPlayerDevotions() {
        for (Player player : getServer().getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            FavorManager favorManager = playerDevotions.get(playerUUID);
            if (favorManager != null) {
                devotionStorage.savePlayerDevotion(playerUUID, favorManager);
            }
        }
    }

}

