package me.xidentified.devotions.storage.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import org.bukkit.Location;

public interface IStorage {

    void savePlayerDevotion(UUID playerUniqueId, FavorManager favorManager);

    DevotionData getPlayerDevotion(UUID playerUUID);

    void removePlayerDevotion(UUID playerUUID);

    List<Shrine> loadAllShrines(DevotionManager devotionManager);

    void removeShrine(Location location, UUID playerId);

    void saveShrine(Shrine newShrine);

    Set<UUID> getAllStoredPlayerUUIDs();

    void closeConnection();
}
