package me.xidentified.devotions.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.FavorManager;
import me.xidentified.devotions.storage.model.DevotionData;
import me.xidentified.devotions.storage.model.IStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

// Store player devotions (what god they're following) and their favor amounts
public class YamlStorage implements IStorage {

    private final Devotions plugin;
    private final File devotionFile;
    private final YamlConfiguration yaml;

    public YamlStorage(Devotions plugin, StorageManager storageManager) {
        this.plugin = plugin;
        devotionFile = new File(storageManager.getStorageFolder(), "storage.yml");
        if (!devotionFile.exists()) {
            try {
                devotionFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        yaml = YamlConfiguration.loadConfiguration(devotionFile);
    }

    public void savePlayerDevotion(UUID playerUniqueId, FavorManager devotion) {
        String key = "playerdata." + playerUniqueId;
        yaml.set(key + ".deity", devotion.getDeity().getName());
        yaml.set(key + ".favor", devotion.getFavor());
        save();
    }

    public DevotionData getPlayerDevotion(UUID playerUUID) {
        String key = "playerdata." + playerUUID.toString();
        String deityName = yaml.getString(key + ".deity");
        int favor = yaml.getInt(key + ".favor", 0);  // Returns 0 if not found
        return new DevotionData(deityName, favor);
    }

    public void removePlayerDevotion(UUID playerUUID) {
        String key = "playerdata." + playerUUID.toString();
        yaml.set(key, null);
        save();
    }

    // Shrine stuff below
    public void saveShrine(Shrine shrine) {
        String key = generateShrineKey(shrine);
        String deityName = shrine.getDeity().getName();

        // Save the shrine details in a single line format
        yaml.set("shrines." + key, deityName);
        save();
    }

    public void removeShrine(Location location, UUID ownerUUID) {
        String key = findKeyByLocationAndOwner(location, ownerUUID);
        if (key != null) {
            yaml.set("shrines." + key, null);
            save();
        }
    }

    private String generateShrineKey(Shrine shrine) {
        Location location = shrine.getLocation();
        UUID ownerUUID = shrine.getOwner();
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ() + "," +
                ownerUUID.toString();
    }

    private String findKeyByLocationAndOwner(Location location, UUID ownerUUID) {
        ConfigurationSection shrinesSection = yaml.getConfigurationSection("shrines");
        if (shrinesSection == null) {
            return null;
        }

        for (String key : shrinesSection.getKeys(false)) {
            String[] parts = key.split(",");
            if (parts.length < 5) {
                continue; // Skip if the format is incorrect
            }

            World world = Bukkit.getWorld(parts[0]);
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            UUID storedOwnerUUID = UUID.fromString(parts[4]);

            Location storedLocation = new Location(world, x, y, z);
            if (storedLocation.equals(location) && storedOwnerUUID.equals(ownerUUID)) {
                return key;
            }
        }

        return null;
    }

    public List<Shrine> loadAllShrines(DevotionManager devotionManager) {
        List<Shrine> loadedShrines = new ArrayList<>();
        ConfigurationSection shrineSection = yaml.getConfigurationSection("shrines");
        if (shrineSection == null) {
            plugin.debugLog("Shrine section is null.");
            return loadedShrines;
        }

        for (String shrineKey : shrineSection.getKeys(false)) {
            String[] parts = shrineKey.split(",");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                plugin.debugLog("World not found: " + parts[0]);
                continue;
            }
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            UUID ownerUUID = UUID.fromString(parts[4]);

            String deityName = shrineSection.getString(shrineKey);
            Deity deity = devotionManager.getDeityByInput(deityName);
            if (deity == null) {
                plugin.debugLog("Deity not found: " + deityName);
                continue;
            }

            Location location = new Location(world, x, y, z);
            Shrine shrine = new Shrine(location, deity, ownerUUID);
            loadedShrines.add(shrine);
        }

        plugin.getLogger().info("Loaded " + loadedShrines.size() + " shrines.");
        return loadedShrines;
    }

    private void save() {
        try {
            yaml.save(devotionFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<UUID> getAllStoredPlayerUUIDs() {
        ConfigurationSection section = yaml.getConfigurationSection("playerdata");
        if (section != null) {
            return section.getKeys(false).stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    public void closeConnection() {
        // Not necessary for yaml :P
    }
}
