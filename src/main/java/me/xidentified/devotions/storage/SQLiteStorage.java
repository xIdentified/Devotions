package me.xidentified.devotions.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.storage.model.DevotionData;
import me.xidentified.devotions.storage.model.IStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class SQLiteStorage implements IStorage {

    private Connection connection;
    private final Devotions plugin;

    public SQLiteStorage(Devotions plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Connect to SQLite database
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/storage/data.db");

            // Create tables if they don't exist
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            // Create playerdata table
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS playerdata ("
                    + "player_uuid TEXT PRIMARY KEY,"
                    + "deity_name TEXT,"
                    + "favor INTEGER"
                    + ");");

            // Create shrines table
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS shrines ("
                    + "location TEXT PRIMARY KEY,"
                    + "deity_name TEXT,"
                    + "owner_uuid TEXT"
                    + ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerDevotion(UUID playerUniqueId, FavorManager devotion) {
        String query = "REPLACE INTO playerdata (player_uuid, deity_name, favor) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUniqueId.toString());
            statement.setString(2, devotion.getDeity().getName());
            statement.setInt(3, devotion.getFavor());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DevotionData getPlayerDevotion(UUID playerUUID) {
        String query = "SELECT deity_name, favor FROM playerdata WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String deityName = resultSet.getString("deity_name");
                int favor = resultSet.getInt("favor");
                return new DevotionData(deityName, favor);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removePlayerDevotion(UUID playerUUID) {
        String query = "DELETE FROM playerdata WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Shrine> loadAllShrines(DevotionManager devotionManager) {
        List<Shrine> shrines = new ArrayList<>();
        String query = "SELECT location, deity_name, owner_uuid FROM shrines";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                String locationStr = resultSet.getString("location");
                Location location = parseLocation(locationStr);
                String deityName = resultSet.getString("deity_name");
                UUID ownerUUID = UUID.fromString(resultSet.getString("owner_uuid"));
                Deity deity = devotionManager.getDeityByInput(deityName);
                if (deity != null) {
                    shrines.add(new Shrine(location, deity, ownerUUID));
                } else {
                    plugin.debugLog("Deity not found: " + deityName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shrines;
    }

    public void removeShrine(Location location, UUID playerId) {
        String query = "DELETE FROM shrines WHERE location = ? AND owner_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, serializeLocation(location));
            statement.setString(2, playerId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveShrine(Shrine newShrine) {
        String query = "REPLACE INTO shrines (location, deity_name, owner_uuid) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, serializeLocation(newShrine.getLocation()));
            statement.setString(2, newShrine.getDeity().getName());
            statement.setString(3, newShrine.getOwner().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<UUID> getAllStoredPlayerUUIDs() {
        Set<UUID> playerUUIDs = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT player_uuid FROM playerdata")) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String playerUUIDString = resultSet.getString("player_uuid");
                playerUUIDs.add(UUID.fromString(playerUUIDString));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playerUUIDs;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String serializeLocation(Location location) {
        // Serialize location to a string, "world,x,y,z"
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + ","
                + location.getBlockZ();
    }

    private Location parseLocation(String locationStr) {
        // Parse location from string
        String[] parts = locationStr.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]));
    }
}
