package me.xidentified.devotions.storage;

import me.xidentified.devotions.Devotions;

import java.io.File;

public class StorageManager {
    private File storageFolder;
    private final Devotions plugin;

    public StorageManager(Devotions plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        storageFolder = new File(plugin.getDataFolder(), "storage");
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }
    }

    public File getStorageFolder() {
        return storageFolder;
    }
}
