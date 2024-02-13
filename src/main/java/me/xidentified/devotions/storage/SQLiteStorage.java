package me.xidentified.devotions.storage;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.managers.FavorManager;

import java.sql.*;
import java.util.*;

public class SQLiteStorage {
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
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/data.db");

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

    public FavorManager getPlayerDevotion(UUID playerUUID) {
        String query = "SELECT deity_name, favor FROM playerdata WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUUID.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String deityName = resultSet.getString("deity_name");
                    Deity deity = plugin.getDevotionManager().getDeityByName(deityName);
                    return new FavorManager(plugin, playerUUID, deity);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Implement methods for saving and removing shrines

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

