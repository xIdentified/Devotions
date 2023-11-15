package me.xidentified.devotions.storage;

import me.xidentified.devotions.Deity;
import me.xidentified.devotions.Shrine;
import me.xidentified.devotions.managers.DevotionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ShrineStorage {
    private final File shrineFile;
    private final YamlConfiguration yaml;

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
    }

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
        if (shrinesSection == null) return null;

        for (String key : shrinesSection.getKeys(false)) {
            String[] parts = key.split(",");
            if (parts.length < 5) continue; // Skip if the format is incorrect

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
        Bukkit.getLogger().log(Level.WARNING, "Inside loadAllShrines");

        List<Shrine> loadedShrines = new ArrayList<>();
        ConfigurationSection shrineSection = yaml.getConfigurationSection("shrines");
        if (shrineSection == null) {
            System.out.println("Shrine section is null."); // Debug log
            return loadedShrines;
        }

        for (String shrineKey : shrineSection.getKeys(false)) {
            String[] parts = shrineKey.split(",");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                System.out.println("World not found: " + parts[0]); // Debug log
                continue;
            }
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            UUID ownerUUID = UUID.fromString(parts[4]);

            String deityName = shrineSection.getString(shrineKey);
            Deity deity = devotionManager.getDeityByName(deityName);
            if (deity == null) {
                System.out.println("Deity not found: " + deityName); // Debug log
                continue;
            }

            Location location = new Location(world, x, y, z);
            Shrine shrine = new Shrine(location, deity, ownerUUID);

            Bukkit.getLogger().log(Level.WARNING, "Owner UUID for shrine: " + ownerUUID);

            loadedShrines.add(shrine);
        }

        System.out.println("Loaded Shrines: " + loadedShrines.size()); // Debug log
        return loadedShrines;
    }

    private void save() {
        try {
            yaml.save(shrineFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}