package me.xidentified.devotions.storage;

import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.storage.model.DevotionData;
import me.xidentified.devotions.storage.model.IStorage;
import org.bukkit.Location;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

public class MySQLStorage implements IStorage {
    private final Devotions plugin;
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public MySQLStorage(Devotions plugin, String host, String database, String username, String password) {
        this.plugin = plugin;
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        initializeDatabase();
    }

    private void initializeDatabase() {
        // Implement MySQL database initialization logic
        // Example: Establish connection, create tables if not exists, etc.
    }

    @Override
    public void savePlayerDevotion(UUID playerUniqueId, DevotionData devotionData) {

    }

    @Override
    public DevotionData getPlayerDevotion(UUID playerUUID) {
        return null;
    }

    @Override
    public void removePlayerDevotion(UUID playerUUID) {

    }

    @Override
    public List<Shrine> loadAllShrines(DevotionManager devotionManager) {
        return null;
    }

    @Override
    public void removeShrine(Location location, UUID playerId) {

    }

    @Override
    public void saveShrine(Shrine newShrine) {

    }

}
