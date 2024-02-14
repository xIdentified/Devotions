package me.xidentified.devotions.storage;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.storage.model.DevotionData;
import me.xidentified.devotions.storage.model.IStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.Connection;
import java.util.*;

import java.sql.*;

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
        try {
            // Establish connection to MySQL database
            connection = DriverManager.getConnection("jdbc:mysql://" + host + "/" + database, username, password);

            // Create tables if they don't exist
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            // Create playerdata table
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS playerdata ("
                    + "player_uuid VARCHAR(36) PRIMARY KEY,"
                    + "deity_name VARCHAR(255),"
                    + "favor INT"
                    + ");");

            // Create shrines table
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS shrines ("
                    + "location VARCHAR(255) PRIMARY KEY,"
                    + "deity_name VARCHAR(255),"
                    + "owner_uuid VARCHAR(36)"
                    + ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerDevotion(UUID playerUniqueId, FavorManager favorManager) {
        String query = "REPLACE INTO playerdata (player_uuid, deity_name, favor) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUniqueId.toString());
            statement.setString(2, favorManager.getDeity().getName());
            statement.setInt(3, favorManager.getFavor());
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
                Deity deity = devotionManager.getDeityByName(deityName);
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
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT player_uuid FROM playerdata")) {
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
        // Serialize location to a string, e.g., "world,x,y,z"
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Location parseLocation(String locationStr) {
        // Parse location from the serialized string
        String[] parts = locationStr.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}