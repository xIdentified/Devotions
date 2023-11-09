package me.xidentified.devotions.storage;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import me.xidentified.devotions.managers.ShrineManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.xidentified.devotions.util.MessageUtils.locationToString;

public class ShrineStorage {
    private final File shrineFile;
    private final YamlConfiguration yaml;
    private int shrineCounter;

    public ShrineStorage(StorageManager storageManager) {
        shrineFile = new File(storageManager.getStorageFolder(), "shrines.yml");
        if (!shrineFile.exists()) {
            try {
                shrineFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        yaml = YamlConfiguration.loadConfiguration(shrineFile);
        shrineCounter = yaml.getInt("shrineCounter", 1); // Default to 1 if not found
    }

    public void saveShrine(Shrine shrine) {
        String key = "shrines." + locationToKey(shrine.getLocation());

        // Check if the shrine exists to decide whether to increment the counter
        if (yaml.get(key) == null) {
            yaml.set("shrineCounter", ++shrineCounter);
        }

        // Save the shrine details
        yaml.set(key + ".owner", shrine.getOwner().getUniqueId().toString());
        yaml.set(key + ".deityName", shrine.getDeity().getName());
        yaml.set(key + ".location", locationToString(shrine.getLocation()));
        save();
    }

    public void removeShrine(Location location) {
        String key = "shrines." + locationToKey(location);
        yaml.set(key, null);
        save();
    }

    private String locationToKey(Location location) {
        // This method generates a string key based on location coordinates
        // It should match the format used in the saveShrine method
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }


    public List<Shrine> loadAllShrines(ShrineManager shrineManager, DevotionManager devotionManager) {
        List<Shrine> loadedShrines = new ArrayList<>();
        ConfigurationSection shrineSection = yaml.getConfigurationSection("shrines");
        if (shrineSection != null) {
            loadedShrines.clear();
            for (String shrineKey : shrineSection.getKeys(false)) {
                String baseKey = "shrines." + shrineKey;
                String ownerUUIDString = yaml.getString(baseKey + ".owner");
                if (ownerUUIDString == null || ownerUUIDString.isEmpty()) {
                    continue; // Skip if the owner's UUID is null or empty
                }
                UUID ownerUUID = UUID.fromString(ownerUUIDString);
                Player owner = Bukkit.getPlayer(ownerUUID);
                Deity deity = devotionManager.getDeityByName(yaml.getString(baseKey + ".deityName"));

                String locString = yaml.getString(baseKey + ".location");
                if (locString == null) {
                    continue; // Skip if the location is not found
                }
                Location location = stringToLocation(locString);
                if (location == null) {
                    continue; // Skip if the location string cannot be parsed into a Location object
                }

                Shrine shrine = new Shrine(location, deity, owner);
                loadedShrines.add(shrine);
            }
        }
        return loadedShrines;
    }

    private Location stringToLocation(String locString) {
        try {
            String[] parts = locString.split(",");
            World world = Bukkit.getWorld(parts[0]);
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void save() {
        yaml.set("shrineCounter", shrineCounter);
        try {
            yaml.save(shrineFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}