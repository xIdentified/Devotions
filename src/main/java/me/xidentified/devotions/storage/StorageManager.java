package me.xidentified.devotions.storage;

import lombok.Getter;
import me.xidentified.devotions.Devotions;
import me.xidentified.devotions.storage.model.IStorage;

import java.io.File;

@Getter
public class StorageManager {
    private File storageFolder;
    private final Devotions plugin;
    private IStorage storage;

    public StorageManager(Devotions plugin, IStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        init();
    }

    private void init() {
        storageFolder = new File(plugin.getDataFolder(), "storage");
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }

        // Initialize the chosen storage type
        String storageType = plugin.getConfig().getString("storage.type", "yaml").toLowerCase();
        if ("mysql".equals(storageType)) {
            this.storage = new MySQLStorage(plugin, "host", "database", "username", "password");
        } else if ("sqlite".equals(storageType)) {
            this.storage = new SQLiteStorage(plugin);
        } else {
            this.storage = new YamlStorage(plugin, this);
        }
    }

}
