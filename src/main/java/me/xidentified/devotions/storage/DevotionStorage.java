package me.xidentified.devotions.storage;

import me.xidentified.devotions.managers.FavorManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

// Store player devotions (what god they're following) and their favor amounts
public class DevotionStorage {
    private final File devotionFile;
    private final YamlConfiguration yaml;

    public DevotionStorage(StorageManager storageManager) {
        devotionFile = new File(storageManager.getStorageFolder(), "playerdata.yml");
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

    private void save() {
        try {
            yaml.save(devotionFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConfigurationSection getYaml() {
        return yaml;
    }
}


