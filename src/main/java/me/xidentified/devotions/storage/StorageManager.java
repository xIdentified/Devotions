package me.xidentified.devotions.storage;

import lombok.Getter;
import me.xidentified.devotions.Devotions;

import java.io.File;

public class StorageManager {
    @Getter
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

}
