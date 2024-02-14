package me.xidentified.devotions.storage.model;

import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.storage.model.DevotionData;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IStorage {
    void savePlayerDevotion(UUID playerUniqueId, FavorManager favorManager);
    DevotionData getPlayerDevotion(UUID playerUUID);
    void removePlayerDevotion(UUID playerUUID);
    List<Shrine> loadAllShrines(DevotionManager devotionManager);
    void removeShrine(Location location, UUID playerId);
    void saveShrine(Shrine newShrine);
    Set<UUID> getAllStoredPlayerUUIDs();
}

